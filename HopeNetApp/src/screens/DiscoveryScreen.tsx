import React, { useState, useEffect, useRef } from 'react';
import { 
  View, Text, StyleSheet, Animated, Easing, TouchableOpacity, 
  FlatList, StatusBar, NativeEventEmitter, NativeModules, LogBox 
} from 'react-native';
import { startDiscovery, saveDiscoveredDevice, clearFoundDevices } from '../services/discoveryService';

LogBox.ignoreAllLogs();

const bleManagerEmitter = new NativeEventEmitter(NativeModules.BleManager);

export default function DiscoveryScreen({ navigation }: any) { // ADDED NAVIGATION
  const [foundNodes, setFoundNodes] = useState<any[]>([]);
  const [isScanning, setIsScanning] = useState(false);
  const [debugLog, setDebugLog] = useState("RADIO IDLE");
  
  const rotateAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.timing(rotateAnim, { 
        toValue: 1, duration: 4000, easing: Easing.linear, useNativeDriver: true 
      })
    ).start();

    const handler = bleManagerEmitter.addListener('BleManagerDiscoverPeripheral', (device: any) => {
      const deviceId = device.id;
      const rawName = device.name || device.advertising?.localName || "";
      const deviceName = rawName.toUpperCase();
      
      const TARGET_UUID = '74278B90-3444-42F0-9F55-0C58A694D87B';
      const advertisedUUIDs = device.advertising?.serviceUUIDs || [];
      const hasOurUUID = advertisedUUIDs.some((u: string) => u.toUpperCase() === TARGET_UUID);
      const isNamed = deviceName.includes('HN-') || deviceName.includes('HNET');

      const isHopeNet = isNamed || hasOurUUID;

      // Filter non-HopeNet devices
      if (!isHopeNet) return; 
      
      let finalName = "Unknown User";
      if (isNamed) {
        // Removes 'HN-' and keeps the rest (which is now the 10 digit number)
        finalName = rawName.replace('HN-', '').replace('HNET-', '');
      } else if (hasOurUUID) {
        finalName = "Hidden Node";
      }

      setFoundNodes(prev => {
        const existingIndex = prev.findIndex(n => n.id === deviceId);
        let newList = [...prev];

        if (existingIndex > -1) {
          newList[existingIndex] = { 
            ...newList[existingIndex], 
            rssi: device.rssi,
            resolvedName: finalName 
          };
        } else {
          saveDiscoveredDevice(device);
          const newNode = { ...device, resolvedName: finalName, isVerified: true };
          newList.push(newNode);
        }
        return newList.sort((a, b) => b.rssi - a.rssi);
      });
    });

    handleManualScan(); 
    return () => handler.remove();
  }, []);

  const handleManualScan = async () => {
    setFoundNodes([]);
    setIsScanning(true);
    setDebugLog("SEARCHING FOR PEERS...");
    await clearFoundDevices();
    await startDiscovery();
    setTimeout(() => { setIsScanning(false); setDebugLog("SCAN COMPLETE"); }, 10000);
  };

  // --- NEW CONNECT FUNCTION ---
  const handleConnect = (device: any) => {
    setIsScanning(false);
    navigation.navigate('Connection', { deviceName: device.resolvedName });
  };

  const spin = rotateAnim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });

  return (
    <View style={styles.container}>
      <StatusBar hidden />
      <View style={styles.header}>
        <Text style={styles.logoText}>HOPE NET DISCOVERY</Text>
        <Text style={styles.debugText}>{debugLog}</Text>
      </View>

      <View style={styles.radarContainer}>
        <View style={styles.circle} />
        {isScanning && <Animated.View style={[styles.sweep, { transform: [{ rotate: spin }] }]} />}
        <View style={styles.centerDot} />
      </View>

      <View style={styles.bottomSheet}>
        <View style={styles.handle} />
        <Text style={styles.sheetTitle}>Nearby Peers ({foundNodes.length})</Text>

        <FlatList
          data={foundNodes}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <View style={[styles.nodeItem, styles.hopeNetCard]}>
              <View style={styles.row}>
                <View style={[styles.icon, styles.iconActive]}>
                    <Text>📡</Text>
                </View>
                <View style={{flex: 1}}>
                    <Text style={[styles.nodeName, {color: '#00f2ff', fontSize: 16}]}>
                        User: {item.resolvedName}
                    </Text>
                    <Text style={styles.nodeSub}>
                      ✅ VERIFIED PEER | {item.rssi} dBm
                    </Text>
                </View>
                
                {/* CONNECT BUTTON TRIGGERS NAVIGATION */}
                <TouchableOpacity 
                  style={styles.badge} 
                  onPress={() => handleConnect(item)}
                >
                    <Text style={styles.badgeText}>CONNECT</Text>
                </TouchableOpacity>
                
              </View>
            </View>
          )}
          ListEmptyComponent={
            <Text style={styles.emptyText}>
                {isScanning ? "Scanning for HopeNet signals..." : "No peers found nearby."}
            </Text>
          }
        />
      </View>

      <TouchableOpacity style={styles.fab} onPress={handleManualScan} disabled={isScanning}>
        <Text style={styles.fabIcon}>{isScanning ? "⏳" : "🌀"}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0a1a1a' },
  header: { padding: 25, paddingTop: 50 },
  logoText: { color: '#fff', fontSize: 18, fontWeight: 'bold', letterSpacing: 2 },
  debugText: { color: '#00f2ff', fontSize: 10, fontFamily: 'monospace', marginTop: 5 },
  radarContainer: { flex: 0.5, justifyContent: 'center', alignItems: 'center' },
  circle: { position: 'absolute', width: 220, height: 220, borderRadius: 110, borderWidth: 1, borderColor: 'rgba(0, 242, 255, 0.15)' },
  centerDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#00f2ff', elevation: 10 },
  sweep: { position: 'absolute', width: 110, height: 110, borderTopLeftRadius: 110, backgroundColor: 'rgba(0, 242, 255, 0.1)', top: '25%', left: '25%' },
  bottomSheet: { backgroundColor: '#0e1111', flex: 1, borderTopLeftRadius: 35, padding: 25, borderWidth: 1, borderColor: '#1a1d1d' },
  handle: { width: 40, height: 4, backgroundColor: '#333', borderRadius: 2, alignSelf: 'center', marginBottom: 20 },
  sheetTitle: { color: '#fff', fontSize: 18, fontWeight: 'bold', marginBottom: 20 },
  nodeItem: { backgroundColor: 'rgba(255,255,255,0.02)', padding: 15, borderRadius: 12, marginBottom: 10, borderWidth: 1, borderColor: '#222' },
  hopeNetCard: { borderColor: '#00f2ff', backgroundColor: 'rgba(0, 242, 255, 0.1)', transform: [{scale: 1.02}] },
  row: { flexDirection: 'row', alignItems: 'center' },
  icon: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#1a1d1d', justifyContent: 'center', alignItems: 'center', marginRight: 15 },
  iconActive: { backgroundColor: 'rgba(0, 242, 255, 0.2)' },
  nodeName: { color: '#888', fontWeight: 'bold', fontSize: 15 },
  nodeSub: { color: '#555', fontSize: 10, marginTop: 4 },
  badge: { backgroundColor: 'rgba(0, 242, 255, 0.2)', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 6 },
  badgeText: { color: '#00f2ff', fontSize: 10, fontWeight: 'bold', letterSpacing: 1 },
  emptyText: { color: '#333', textAlign: 'center', marginTop: 40, fontStyle: 'italic' },
  fab: { position: 'absolute', bottom: 30, right: 30, width: 70, height: 70, borderRadius: 35, backgroundColor: '#00f2ff', justifyContent: 'center', alignItems: 'center', elevation: 12 },
  fabIcon: { fontSize: 24 }
});