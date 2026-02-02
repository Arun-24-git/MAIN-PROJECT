import React, { useState } from 'react';
import { View, Text, StyleSheet, TextInput, TouchableOpacity, FlatList, StatusBar, KeyboardAvoidingView, Platform } from 'react-native';

export default function ChatScreen({ route, navigation }: any) {
  const { deviceName } = route.params;
  const [msgText, setMsgText] = useState('');
  const [messages, setMessages] = useState([
    { id: '1', text: 'Base Command', type: 'received', time: '10:42 AM' },
    { id: '2', text: 'Message content encrypted', type: 'sent', time: '10:43 AM', status: 'DELIVERED' },
    { id: '3', text: 'MESH NETWORK ESTABLISHED', type: 'system', time: '10:44 AM' },
  ]);

  const sendMessage = () => {
    if (msgText.trim().length === 0) return;
    
    // Add User Message
    const newMsg = {
        id: Date.now().toString(),
        text: msgText,
        type: 'sent',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: 'DELIVERED' 
    };
    
    setMessages(prev => [...prev, newMsg]);
    setMsgText('');

    // Simulate Reply (Optional demo feature)
    setTimeout(() => {
        setMessages(prev => [...prev, {
            id: (Date.now() + 1).toString(),
            text: "Received secure packet.",
            type: 'received',
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }]);
    }, 2000);
  };

  const renderItem = ({ item }: any) => {
    if (item.type === 'system') {
        return (
            <View style={styles.systemMsgContainer}>
                <Text style={styles.systemMsgText}>📶 {item.text}</Text>
            </View>
        );
    }
    
    const isSent = item.type === 'sent';
    return (
      <View style={[styles.msgWrapper, isSent ? styles.sentWrapper : styles.receivedWrapper]}>
        {!isSent && <View style={styles.avatar}><Text>👤</Text></View>}
        <View style={[styles.bubble, isSent ? styles.sentBubble : styles.receivedBubble]}>
            <Text style={styles.msgText}>{item.text}</Text>
            <View style={styles.metaRow}>
                {isSent && <Text style={styles.status}>{item.status}</Text>}
                <Text style={styles.time}>{item.time}</Text>
            </View>
        </View>
        {isSent && <View style={styles.myAvatar}><Text>👤</Text></View>}
      </View>
    );
  };

  return (
    <View style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#061e1e" />
        
        {/* Header */}
        <View style={styles.header}>
            <TouchableOpacity onPress={() => navigation.goBack()}><Text style={styles.backArrow}>❮</Text></TouchableOpacity>
            <View style={{alignItems: 'center'}}>
                <View style={styles.secureBadge}>
                    <Text style={styles.secureText}>🛡 SECURE NODE</Text>
                </View>
                <Text style={styles.headerTitle}>User {deviceName}</Text>
            </View>
            <Text style={{fontSize: 20, color: '#fff'}}>⋮</Text>
        </View>

        {/* Chat List */}
        <FlatList
            data={messages}
            renderItem={renderItem}
            keyExtractor={item => item.id}
            contentContainerStyle={{ padding: 20 }}
        />

        {/* Input Bar */}
        <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"}>
            <View style={styles.inputContainer}>
                <Text style={styles.attachIcon}>📎</Text>
                <TextInput 
                    style={styles.input}
                    placeholder="Encrypting message..."
                    placeholderTextColor="#555"
                    value={msgText}
                    onChangeText={setMsgText}
                />
                <Text style={{marginRight: 15}}>😊</Text>
                <TouchableOpacity style={styles.micBtn} onPress={sendMessage}>
                    <Text style={{fontSize: 16}}>➤</Text>
                </TouchableOpacity>
            </View>
        </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#061e1e' }, // Darker Teal BG
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 15, paddingTop: 50, backgroundColor: '#041515', borderBottomWidth: 1, borderBottomColor: '#1a2a2a' },
  backArrow: { color: '#fff', fontSize: 24 },
  secureBadge: { backgroundColor: 'rgba(0, 242, 255, 0.1)', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 4, marginBottom: 2 },
  secureText: { color: '#00f2ff', fontSize: 8, fontWeight: 'bold' },
  headerTitle: { color: '#fff', fontWeight: 'bold' },
  
  systemMsgContainer: { alignItems: 'center', marginVertical: 15 },
  systemMsgText: { color: '#00f2ff', backgroundColor: 'rgba(0, 242, 255, 0.1)', padding: 5, borderRadius: 5, fontSize: 10, fontFamily: 'monospace' },

  msgWrapper: { flexDirection: 'row', marginBottom: 15, alignItems: 'flex-end' },
  sentWrapper: { justifyContent: 'flex-end' },
  receivedWrapper: { justifyContent: 'flex-start' },
  
  avatar: { width: 30, height: 30, borderRadius: 15, backgroundColor: '#333', justifyContent: 'center', alignItems: 'center', marginRight: 10 },
  myAvatar: { width: 30, height: 30, borderRadius: 15, backgroundColor: '#00f2ff', justifyContent: 'center', alignItems: 'center', marginLeft: 10 },
  
  bubble: { maxWidth: '75%', padding: 12, borderRadius: 12 },
  receivedBubble: { backgroundColor: '#1a2a2a', borderBottomLeftRadius: 2 },
  sentBubble: { backgroundColor: 'rgba(0, 242, 255, 0.15)', borderBottomRightRadius: 2, borderWidth: 1, borderColor: 'rgba(0, 242, 255, 0.3)' },
  
  msgText: { color: '#fff', fontSize: 14 },
  metaRow: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 5, alignItems: 'center' },
  time: { color: '#666', fontSize: 9, marginLeft: 5 },
  status: { color: '#00f2ff', fontSize: 8, fontWeight: 'bold' },

  inputContainer: { flexDirection: 'row', alignItems: 'center', padding: 10, backgroundColor: '#041515', borderTopWidth: 1, borderTopColor: '#1a2a2a' },
  attachIcon: { color: '#888', fontSize: 20, marginRight: 10 },
  input: { flex: 1, backgroundColor: '#0d1f1f', color: '#fff', borderRadius: 20, paddingHorizontal: 15, paddingVertical: 8, marginRight: 10 },
  micBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#00f2ff', justifyContent: 'center', alignItems: 'center' }
});