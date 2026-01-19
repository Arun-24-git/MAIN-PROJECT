import BlePeripheral from 'react-native-ble-peripheral';
import { PermissionsAndroid, Platform } from 'react-native';

/**
 * USER STORY 4: Advertise device presence.
 * This service tells the phone to act as a beacon.
 * Includes a critical fix for Samsung A12 hardware limitations.
 */
export const startAdvertisingPresence = async (phoneNumber: string) => {
  try {
    // 1. Request Nearby Devices Permissions (Android 12+)
    if (Platform.OS === 'android') {
      await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      ]);
    }

    // 2. Setup the Node Identity (What others see during discovery)
    await BlePeripheral.setName(`HN-${phoneNumber}`);

    // 3. Add the unique HopeNet Mesh Service ID
    const SERVICE_UUID = '12345678-1234-1234-1234-1234567890ab';
    await BlePeripheral.addService(SERVICE_UUID, true);

    // 4. START THE BROADCAST
    // CRITICAL FIX: We use a nested try-catch to target the hardware call.
    // On Samsung A12, the hardware 'Advertiser' is null, which causes the crash.
    try {
      await BlePeripheral.start();
      console.log("📡 MESH: Hardware Advertising started successfully");
      return true; // Actual hardware success
    } catch (hardwareError) {
      // This block prevents the "Red Screen" on your Samsung A12
      console.log("ℹ️ Hardware Note: BLE Advertising not supported by this phone's firmware.");
      return false; // Hardware limitation reached, but app stays alive
    }

  } catch (error) {
    console.error("Discovery Service Error:", error);
    return false;
  }
};