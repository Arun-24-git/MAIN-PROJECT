import BlePeripheral from 'react-native-ble-peripheral';
import BleManager from 'react-native-ble-manager';
import { PermissionsAndroid, Platform } from 'react-native';
import { getDBConnection } from '../database/db';

// --- THE SHARED "SECRET KEY" FOR HOPENET MESH ---
// (Used for filtering, even if not broadcasted in main packet)
const HOPE_NET_UUID = '74278b90-3444-42f0-9f55-0c58a694d87b';

/**
 * USER STORY 4: Advertise device presence.
 * FIX: "Name-Only" Mode to allow full 10-digit phone numbers.
 */
export const startAdvertisingPresence = async (phoneNumber: string) => {
  try {
    if (Platform.OS === 'android') {
      await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      ]);
    }

    // 1. Reset Bluetooth
    try { await BlePeripheral.stop(); } catch(e) {}

    // 2. Generate the 10-digit Name
    // Example: "HN-7994518252"
    const cleanNumber = phoneNumber.replace(/[^0-9]/g, '');
    const shortPhone = cleanNumber.length > 10 ? cleanNumber.slice(-10) : cleanNumber;
    const broadcastName = `HN-${shortPhone}`; 

    // 3. Set Name
    await BlePeripheral.setName(broadcastName);

    // === CRITICAL FIX ===
    // We REMOVED 'addService'. 
    // Sending the UUID + Long Name together exceeds the 31-byte limit.
    // By sending ONLY the name, we ensure the broadcast works on all phones.
    
    // 4. Start Broadcasting
    await BlePeripheral.start();
    
    console.log(`📡 BROADCASTING: ${broadcastName} (Name-Only Mode)`);
    return true;

  } catch (error) {
    console.error("Advertising Service Error:", error);
    return false;
  }
};

/**
 * USER STORY 5: Discover nearby HopeNet users.
 */
export const startDiscovery = async () => {
  try {
    if (Platform.OS === 'android') {
      await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      ]);
    }

    await BleManager.start({ showAlert: false });
    
    // Hardware warm-up
    await new Promise(resolve => setTimeout(resolve, 500));

    // Wide scan
    await BleManager.scan([], 10, true, { 
        matchMode: 1, 
        scanMode: 2, 
        reportDelay: 0 
    }); 
    
    console.log("🔍 MESH: Scanning started...");
  } catch (error) {
    console.error("Discovery Scan Error:", error);
  }
};

/**
 * Database Logic
 */
export const saveDiscoveredDevice = async (device: any) => {
  try {
    const db = await getDBConnection();
    const timestamp = new Date().toISOString();
    
    const name = device.name || device.advertising?.localName || 'Unknown Node';
    const cleanPhone = name.replace('HNET-', '').replace('HN-', '');

    const query = `
      INSERT OR REPLACE INTO devices (device_id, user_id, last_seen, signal_strength, is_reachable) 
      VALUES (?, ?, ?, ?, 1)
    `;
    
    await db.executeSql(query, [device.id, cleanPhone, timestamp, device.rssi]);
    console.log(`💾 MESH: Persisted node ${cleanPhone} (${device.rssi} dBm)`);
  } catch (e) {
    console.error("Database Write Error:", e);
  }
};

export const clearFoundDevices = async () => {
  try {
    const db = await getDBConnection();
    await db.executeSql('DELETE FROM devices');
    console.log("🗑️ MESH: Local device table cleared");
  } catch (error) {
    console.error(error);
  }
};