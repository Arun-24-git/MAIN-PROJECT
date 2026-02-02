import React, { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, StatusBar, Animated, Easing, ScrollView, LogBox } from 'react-native';
import { getDBConnection } from '../database/db';
import { startAdvertisingPresence } from '../services/discoveryService';

LogBox.ignoreAllLogs();

export default function HomeScreen({ navigation }: any) {
  const [userPhone, setUserPhone] = useState('User');
  const [isBroadcasting, setIsBroadcasting] = useState(false);
  const pulseAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const launchNode = async () => {
      try {
        const db = await getDBConnection();
        const results = await db.executeSql(
          'SELECT phone_number FROM users ORDER BY created_at DESC LIMIT 1'
        );
        
        if (results[0].rows.length > 0) {
          const phone = results[0].rows.item(0).phone_number;
          setUserPhone(phone);
          setIsBroadcasting(true); 
          startPulse();

          // START BROADCASTING
          startAdvertisingPresence(phone).then((success) => {
            console.log(success ? "✅ BLE Active" : "❌ BLE Failed");
          });
        }
      } catch (error) { console.error(error); }
    };
    launchNode();
  }, []);

  const startPulse = () => {
    Animated.loop(
      Animated.timing(pulseAnim, {
        toValue: 1, duration: 2000, easing: Easing.out(Easing.ease), useNativeDriver: true,
      })
    ).start();
  };

  const FeatureCard = ({ title, sub, icon, onPress }: any) => (
    <TouchableOpacity style={styles.glassCard} onPress={onPress}>
      <View style={styles.cardIconBox}><Text style={{fontSize: 24}}>{icon}</Text></View>
      <View style={{ flex: 1, marginLeft: 15 }}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.cardSub}>{sub}</Text>
      </View>
      <Text style={styles.arrow}>❯</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0a0b0d" />
      
      <View style={styles.header}>
        <View style={styles.logoRow}>
          <View style={styles.iconWrapper}>
            {isBroadcasting && (
              <Animated.View style={[styles.pulseRing, { 
                transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 2.5] }) }],
                opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.6, 0] }) 
              }]} />
            )}
            <Text style={{fontSize: 26}}>🛰️</Text>
          </View>
          <Text style={styles.logoText}>HOPE NET</Text>
        </View>
        
        <View style={[styles.statusBadge, isBroadcasting && styles.statusBadgeActive]}>
          <View style={[styles.statusDot, isBroadcasting && styles.statusDotActive]} />
          <Text style={styles.statusText}>{isBroadcasting ? "ONLINE" : "OFFLINE"}</Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.welcome}>Welcome,</Text>
        <Text style={styles.phone}>{userPhone}</Text>
        <Text style={styles.mode}>📶 Emergency Mesh Active</Text>
        
        <View style={styles.cardContainer}>
          <FeatureCard title="Discover Nodes" sub="Scan for HopeNet peers" icon="🌀" onPress={() => navigation.navigate('Discovery')} />
          <FeatureCard title="Secure Chats" sub="Coming soon..." icon="💬" onPress={() => {}} />
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0a0b0d' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 50, paddingHorizontal: 20 },
  logoRow: { flexDirection: 'row', alignItems: 'center' },
  iconWrapper: { width: 45, height: 45, justifyContent: 'center', alignItems: 'center' },
  pulseRing: { position: 'absolute', width: 40, height: 40, borderRadius: 20, backgroundColor: '#00f2ff' },
  logoText: { color: '#fff', fontSize: 22, fontWeight: 'bold', marginLeft: 10, letterSpacing: 2 },
  statusBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#1a1d21', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, borderWidth: 1, borderColor: '#333' },
  statusBadgeActive: { borderColor: '#32d74b', backgroundColor: 'rgba(50, 215, 75, 0.1)' },
  statusDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#ff453a', marginRight: 8 },
  statusDotActive: { backgroundColor: '#32d74b' },
  statusText: { color: '#fff', fontSize: 10, fontWeight: 'bold', letterSpacing: 1 },
  content: { paddingHorizontal: 20, marginTop: 40 },
  welcome: { color: '#fff', fontSize: 24, opacity: 0.7 },
  phone: { color: '#fff', fontSize: 36, fontWeight: 'bold' },
  mode: { color: '#ff9500', fontWeight: 'bold', marginTop: 5, marginBottom: 40 },
  cardContainer: { gap: 15 },
  glassCard: { backgroundColor: 'rgba(255, 255, 255, 0.05)', padding: 25, borderRadius: 20, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: 'rgba(255, 255, 255, 0.1)' },
  cardIconBox: { width: 50, height: 50, borderRadius: 12, backgroundColor: 'rgba(0, 242, 255, 0.1)', justifyContent: 'center', alignItems: 'center' },
  cardTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold' },
  cardSub: { color: '#888', fontSize: 13, marginTop: 5 },
  arrow: { color: 'rgba(255, 255, 255, 0.2)', fontSize: 18, marginLeft: 10 },
});