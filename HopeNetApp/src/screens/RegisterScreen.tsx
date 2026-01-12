import React, { useState, useRef, useEffect } from 'react';
import { 
  View, 
  Text, 
  TextInput, 
  Alert, 
  StyleSheet, 
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
  Animated,
  Dimensions,
  Keyboard,
  StatusBar,
  Modal,
  FlatList
} from 'react-native';
import { registerUser } from '../services/userService';

const { width, height } = Dimensions.get('window');

const countryData = [
  { name: 'India', code: '+91', flag: '🇮🇳' },
  { name: 'United States', code: '+1', flag: '🇺🇸' },
  { name: 'United Kingdom', code: '+44', flag: '🇬🇧' },
  { name: 'United Arab Emirates', code: '+971', flag: '🇦🇪' },
  { name: 'Australia', code: '+61', flag: '🇦🇺' },
  { name: 'Canada', code: '+1', flag: '🇨🇦' },
  { name: 'Germany', code: '#49', flag: '🇩🇪' },
  { name: 'Japan', code: '+81', flag: '🇯🇵' },
  { name: 'Pakistan', code: '+92', flag: '🇵🇰' },
  { name: 'Sri Lanka', code: '+94', flag: '🇱🇰' },
];

export default function RegisterScreen() {
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  const [showPicker, setShowPicker] = useState(false);
  const [selectedCountry, setSelectedCountry] = useState(countryData[0]);
  const [searchQuery, setSearchQuery] = useState('');

  const fadeAnim = useRef(new Animated.Value(0)).current;
  const scanLineAnim = useRef(new Animated.Value(0)).current;
  const buttonPulseAnim = useRef(new Animated.Value(1)).current;
  const progressAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(fadeAnim, { toValue: 1, duration: 1000, useNativeDriver: true }).start();
    Animated.loop(
      Animated.sequence([
        Animated.timing(scanLineAnim, { toValue: 1, duration: 3000, useNativeDriver: true }),
        Animated.timing(scanLineAnim, { toValue: 0, duration: 3000, useNativeDriver: true })
      ])
    ).start();
    
    // Only pulse the button when it is active (10 digits entered)
    if(phone.length === 10) {
        Animated.loop(
          Animated.sequence([
            Animated.timing(buttonPulseAnim, { toValue: 1.05, duration: 800, useNativeDriver: true }),
            Animated.timing(buttonPulseAnim, { toValue: 1, duration: 800, useNativeDriver: true })
          ])
        ).start();
    }
  }, [phone]);

  const handleRegister = async () => {
    const fullNumber = selectedCountry.code + phone.trim();
    if (phone.trim().length < 10) {
      Alert.alert("⚠️ INPUT ERROR", "Please enter a valid 10-digit number.");
      return;
    }

    setLoading(true);
    Keyboard.dismiss();
    Animated.timing(progressAnim, { toValue: 1, duration: 3000, useNativeDriver: false }).start();

    try {
      const id = await registerUser(fullNumber);
      Alert.alert(
        "✅ REGISTRATION COMPLETE",
        `Your device is now secured.\n\nPhone: ${fullNumber}\n\nID: ${id.substring(0,8)}...`,
        [{ text: "OK", onPress: () => { setPhone(''); progressAnim.setValue(0); } }]
      );
    } catch (e) {
      Alert.alert("❌ FAILED", "Database storage error.");
    } finally {
      setLoading(false);
    }
  };

  const filteredCountries = countryData.filter(item => 
    item.name.toLowerCase().includes(searchQuery.toLowerCase()) || item.code.includes(searchQuery)
  );

  return (
    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#0a1a1a" />
      
      <Animated.View style={[styles.content, { opacity: fadeAnim }]}>
        
        <View style={styles.header}>
          <View style={styles.iconCircle}>
             <Text style={{fontSize: 40}}>📱</Text>
          </View>
          <Text style={styles.title}>HOPE NET</Text>
          <Text style={styles.subtitle}>OFFLINE MESH REGISTRATION</Text>
          <Animated.View style={[styles.scanLine, { transform: [{ translateY: scanLineAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 120] }) }] }]} />
        </View>

        <View style={styles.card}>
          <View style={styles.cardPadding}>
            <Text style={styles.instruction}>Verify your Phone number </Text>

            <View style={styles.inputSection}>
              <View style={[styles.inputBox, isFocused && styles.inputBoxFocused]}>
                <TouchableOpacity style={styles.countryPickerTrigger} onPress={() => setShowPicker(true)}>
                  <Text style={styles.prefixText}>{selectedCountry.flag} {selectedCountry.code}</Text>
                </TouchableOpacity>

                <TextInput 
                  style={styles.input} 
                  placeholder="00000 00000"
                  placeholderTextColor="#999"
                  value={phone}
                  onChangeText={setPhone}
                  keyboardType="numeric"
                  maxLength={10}
                  onFocus={() => setIsFocused(true)}
                  onBlur={() => setIsFocused(false)}
                  editable={!loading}
                />
              </View>
            </View>

            {loading ? (
              <View style={styles.loadingBox}>
                <ActivityIndicator size="large" color="#00f2ff" />
                <Text style={styles.loadingText}>INITIALIZING RSA IDENTITY...</Text>
                <View style={styles.barContainer}>
                  <Animated.View style={[styles.barFill, { width: progressAnim.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }) }]} />
                </View>
              </View>
            ) : (
              <Animated.View style={{ transform: [{ scale: phone.length === 10 ? buttonPulseAnim : 1 }] }}>
                <TouchableOpacity 
                  onPress={handleRegister}
                  activeOpacity={0.8}
                  style={[styles.btn, phone.length === 10 ? styles.btnActive : styles.btnDisabled]}
                  disabled={phone.length < 10}
                >
                  <Text style={[styles.btnText, phone.length < 10 && { color: '#004d4d' }]}>
                    REGISTER
                  </Text>
                </TouchableOpacity>
              </Animated.View>
            )}
          </View>
        </View>

        <View style={styles.footer}>
          <Text style={styles.footerText}>🛡️ SECURE • DECENTRALIZED • OFFLINE</Text>
        </View>
      </Animated.View>

      <Modal visible={showPicker} animationType="slide" transparent={true}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}><Text style={styles.modalTitle}>Select Country</Text><TouchableOpacity onPress={() => setShowPicker(false)}><Text style={styles.closeText}>CLOSE</Text></TouchableOpacity></View>
            <TextInput style={styles.searchInput} placeholder="Search country..." placeholderTextColor="#999" onChangeText={setSearchQuery}/>
            <FlatList data={filteredCountries} keyExtractor={(item) => item.name} renderItem={({item}) => (
                <TouchableOpacity style={styles.countryItem} onPress={() => { setSelectedCountry(item); setShowPicker(false); setSearchQuery(''); }}>
                    <Text style={styles.countryFlag}>{item.flag}</Text><Text style={styles.countryName}>{item.name}</Text><Text style={styles.countryCodeText}>{item.code}</Text>
                </TouchableOpacity>
            )}/>
          </View>
        </View>
      </Modal>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0a1a1a' },
  content: { flex: 1, padding: 20, justifyContent: 'center' },
  header: { alignItems: 'center', marginBottom: 25 },
  iconCircle: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#002b2b', justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: '#00f2ff', marginBottom: 15 },
  title: { fontSize: 38, fontWeight: '900', color: '#fff', letterSpacing: 4 },
  subtitle: { fontSize: 13, color: '#00f2ff', letterSpacing: 2, opacity: 0.8 },
  scanLine: { position: 'absolute', width: '100%', height: 2, backgroundColor: 'rgba(0, 242, 255, 0.4)', top: 40 },
  
  card: { backgroundColor: '#001a1a', borderRadius: 20, borderWidth: 1, borderColor: '#00f2ff', overflow: 'hidden' },
  cardPadding: { padding: 25 },
  instruction: { fontSize: 14, color: '#fff', fontWeight: 'bold', marginBottom: 15, textAlign: 'center', opacity: 0.9 },
  
  inputSection: { marginBottom: 30 },
  inputBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 12, height: 65, borderWidth: 3, borderColor: '#004d4d' },
  inputBoxFocused: { borderColor: '#00f2ff' },
  countryPickerTrigger: { flexDirection: 'row', alignItems: 'center', paddingLeft: 12, borderRightWidth: 1, borderRightColor: '#ddd', height: '100%', paddingRight: 8 },
  prefixText: { color: '#000', fontSize: 16, fontWeight: 'bold' },
  input: { flex: 1, padding: 15, fontSize: 22, color: '#000', fontWeight: 'bold' },
  
  loadingBox: { alignItems: 'center' },
  loadingText: { color: '#00f2ff', fontSize: 12, marginVertical: 15, fontWeight: 'bold', letterSpacing: 1 },
  barContainer: { width: '100%', height: 4, backgroundColor: '#002b2b', borderRadius: 2 },
  barFill: { height: '100%', backgroundColor: '#00f2ff', borderRadius: 2 },
  
  btn: { 
    padding: 20, 
    borderRadius: 12, 
    alignItems: 'center', 
    justifyContent: 'center',
    shadowColor: '#00f2ff',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 10,
    elevation: 8,
  },
  btnActive: { 
    backgroundColor: '#00f2ff', // High-visibility Cyan
  },
  btnDisabled: { 
    backgroundColor: '#002b2b', 
    borderWidth: 1, 
    borderColor: '#004d4d',
    elevation: 0
  },
  btnText: { 
    color: '#000', // Black text for high contrast on active button
    fontWeight: '900', 
    fontSize: 20,
    letterSpacing: 2
  },
  
  footer: { alignItems: 'center', marginTop: 30 },
  footerText: { fontSize: 11, color: 'rgba(0, 242, 255, 0.4)', letterSpacing: 1 },

  // Modal styles
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.85)', justifyContent: 'flex-end' },
  modalContent: { backgroundColor: '#fff', height: height * 0.7, borderTopLeftRadius: 30, borderTopRightRadius: 30, padding: 25 },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
  modalTitle: { fontSize: 22, fontWeight: 'bold', color: '#000' },
  closeText: { color: '#007AFF', fontWeight: 'bold', fontSize: 16 },
  searchInput: { backgroundColor: '#f5f5f5', padding: 15, borderRadius: 12, marginBottom: 20, color: '#000', fontSize: 16 },
  countryItem: { flexDirection: 'row', paddingVertical: 18, borderBottomWidth: 1, borderBottomColor: '#f0f0f0', alignItems: 'center' },
  countryFlag: { fontSize: 28, marginRight: 15 },
  countryName: { flex: 1, fontSize: 17, color: '#333' },
  countryCodeText: { fontSize: 17, fontWeight: 'bold', color: '#666' }
});