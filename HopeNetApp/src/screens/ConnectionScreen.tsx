import React, { useEffect, useRef, useState } from 'react';
import { View, Text, StyleSheet, Animated, Easing, TouchableOpacity, StatusBar } from 'react-native';

export default function ConnectionScreen({ route, navigation }: any) {
  const { deviceName } = route.params;
  const [status, setStatus] = useState("ESTABLISHING SECURE P2P CHANNEL...");
  const [isSecure, setIsSecure] = useState(false);
  
  // Animation Values
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const progressAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    // 1. Start Pulse
    Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1.2, duration: 1000, easing: Easing.ease, useNativeDriver: true }),
        Animated.timing(pulseAnim, { toValue: 1, duration: 1000, easing: Easing.ease, useNativeDriver: true })
      ])
    ).start();

    // 2. Simulate Connection Logic
    setTimeout(() => {
        setStatus("EXCHANGING KEYS...");
    }, 1500);

    setTimeout(() => {
        setIsSecure(true);
        setStatus("CONNECTION SUCCESS");
    }, 3500);

    // 3. Auto Navigate to Chat
    setTimeout(() => {
        navigation.replace('Chat', { deviceName });
    }, 4500);

  }, []);

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0a0b0d" />
      
      {/* Header */}
      <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
        <Text style={styles.backText}>❮</Text>
      </TouchableOpacity>
      <Text style={styles.title}>Connecting to User {deviceName}</Text>

      {/* Central Animation */}
      <View style={styles.center}>
        <Animated.View style={[styles.ring, { transform: [{ scale: pulseAnim }], borderColor: isSecure ? '#32d74b' : '#00f2ff' }]} />
        <View style={styles.iconBox}>
            <Text style={{fontSize: 40}}>{isSecure ? "🔐" : "🔒"}</Text>
        </View>
      </View>

      {/* Status Text */}
      <View style={styles.statusBox}>
        <Text style={[styles.statusText, isSecure && { color: '#32d74b' }]}>{status}</Text>
        <View style={styles.encryptionBadge}>
            <Text style={styles.encText}>Encryption Enabled 🔒</Text>
        </View>
      </View>

      {/* Success Badge */}
      {isSecure && (
        <View style={styles.successBadge}>
            <Text style={styles.successText}>✔ Connection Success</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0a0b0d', alignItems: 'center', padding: 20 },
  backBtn: { position: 'absolute', top: 50, left: 20, zIndex: 10 },
  backText: { color: '#fff', fontSize: 24 },
  title: { color: '#fff', fontSize: 18, marginTop: 40, fontWeight: 'bold' },
  
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  ring: { position: 'absolute', width: 200, height: 200, borderRadius: 100, borderWidth: 2, opacity: 0.5 },
  iconBox: { width: 100, height: 100, borderRadius: 50, backgroundColor: 'rgba(255,255,255,0.05)', justifyContent: 'center', alignItems: 'center' },
  
  statusBox: { marginBottom: 50, alignItems: 'center' },
  statusText: { color: '#00f2ff', fontSize: 12, letterSpacing: 2, marginBottom: 15, fontWeight: 'bold' },
  encryptionBadge: { backgroundColor: 'rgba(0, 242, 255, 0.1)', paddingHorizontal: 15, paddingVertical: 8, borderRadius: 20 },
  encText: { color: '#00f2ff', fontSize: 10 },

  successBadge: { position: 'absolute', bottom: 40, width: '100%', backgroundColor: 'rgba(50, 215, 75, 0.2)', padding: 15, borderRadius: 10, alignItems: 'center', borderWidth: 1, borderColor: '#32d74b' },
  successText: { color: '#32d74b', fontWeight: 'bold' }
});