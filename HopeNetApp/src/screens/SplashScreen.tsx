import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, Animated, Dimensions, StatusBar } from 'react-native';

const { width } = Dimensions.get('window');

const SplashScreen = ({ onFinish }: { onFinish: () => void }) => {
  const progress = useRef(new Animated.Value(0)).current;
  const rotateValue = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    // 1. Radar Animation (Rotating the sweep)
    Animated.loop(
      Animated.timing(rotateValue, {
        toValue: 1,
        duration: 3000,
        useNativeDriver: true,
      })
    ).start();

    // 2. Loading Bar Animation (0 to 100%)
    Animated.timing(progress, {
      toValue: width * 0.7, // 70% of screen width
      duration: 3500,
      useNativeDriver: false,
    }).start(() => {
      onFinish(); // Move to next screen when done
    });
  }, []);

  const spin = rotateValue.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  return (
    <View style={styles.container}>
      <StatusBar hidden />
      
      {/* Satellite Icon Placeholder */}
      <View style={styles.iconCircle}>
        <Text style={{ fontSize: 30 }}>🛰️</Text>
      </View>

      <Text style={styles.title}>HOPE NET</Text>
      <Text style={styles.subtitle}>OFFLINE EMERGENCY MESSAGING</Text>

      {/* Radar Graphic */}
      <View style={styles.radarContainer}>
        <View style={[styles.radarCircle, { width: 220, height: 220, opacity: 0.2 }]} />
        <View style={[styles.radarCircle, { width: 140, height: 140, opacity: 0.4 }]} />
        <View style={[styles.radarCircle, { width: 60, height: 60, opacity: 0.6 }]} />
        
        {/* Radar Sweep Animation */}
        <Animated.View style={[styles.sweep, { transform: [{ rotate: spin }] }]} />
        
        {/* Center Point */}
        <View style={styles.centerDot} />
      </View>

      {/* Loading Section */}
      <View style={styles.loadingWrapper}>
        <Text style={styles.loadingText}>[ LOADING... ]</Text>
        <View style={styles.progressBarBg}>
          <Animated.View style={[styles.progressBarFill, { width: progress }]} />
        </View>
      </View>

      <Text style={styles.footer}>🛡️ MESH NETWORK: SECURE</Text>
      <Text style={styles.version}>v1.0.4</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#0a1a1a', alignItems: 'center', justifyContent: 'center' },
  iconCircle: { width: 60, height: 60, borderRadius: 30, backgroundColor: '#002b2b', justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: '#00f2ff', marginBottom: 20, elevation: 10 },
  title: { color: '#fff', fontSize: 32, fontWeight: 'bold', letterSpacing: 8 },
  subtitle: { color: '#00f2ff', fontSize: 12, marginTop: 5, letterSpacing: 2, opacity: 0.7 },
  radarContainer: { width: 250, height: 250, justifyContent: 'center', alignItems: 'center', marginVertical: 40 },
  radarCircle: { position: 'absolute', borderRadius: 150, borderWidth: 1, borderColor: '#00f2ff' },
  centerDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: '#fff', elevation: 10 },
  sweep: { position: 'absolute', width: 110, height: 110, borderTopLeftRadius: 110, backgroundColor: 'rgba(0, 242, 255, 0.1)', top: 15, left: 15 },
  loadingWrapper: { marginTop: 40, alignItems: 'center' },
  loadingText: { color: '#00f2ff', fontSize: 18, letterSpacing: 4, marginBottom: 15 },
  progressBarBg: { width: width * 0.7, height: 4, backgroundColor: '#002b2b' },
  progressBarFill: { height: '100%', backgroundColor: '#00f2ff' },
  footer: { position: 'absolute', bottom: 60, color: '#00f2ff', fontSize: 12, opacity: 0.5, letterSpacing: 1 },
  version: { position: 'absolute', bottom: 40, color: '#00f2ff', fontSize: 10, opacity: 0.3 },
});

export default SplashScreen;