import React, { useState, useEffect } from 'react';
import { View } from 'react-native';
import { initDatabase } from './src/database/db';
import RegisterScreen from './src/screens/RegisterScreen';
import SplashScreen from './src/screens/SplashScreen';

export default function App() {
  const [isAppReady, setIsAppReady] = useState(false);

  useEffect(() => {
    // Initialize Database while splash screen is showing
    initDatabase();
  }, []);

  return (
    <View style={{ flex: 1 }}>
      {isAppReady ? (
        // Once loading is done, show registration
        <RegisterScreen />
      ) : (
        // Start with splash screen
        <SplashScreen onFinish={() => setIsAppReady(true)} />
      )}
    </View>
  );
}