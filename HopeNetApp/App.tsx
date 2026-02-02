import React, { useState, useEffect } from 'react';
import { LogBox } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { initDatabase, getDBConnection } from './src/database/db';

// Import Screens
import SplashScreen from './src/screens/SplashScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import HomeScreen from './src/screens/HomeScreen';
import DiscoveryScreen from './src/screens/DiscoveryScreen'; 
// --- NEW SCREENS ---
import ConnectionScreen from './src/screens/ConnectionScreen';
import ChatScreen from './src/screens/ChatScreen';

// Hide warning bar for a professional demo
LogBox.ignoreAllLogs();

const Stack = createNativeStackNavigator();

export default function App() {
  const [isReady, setIsReady] = useState(false);
  const [hasAccount, setHasAccount] = useState(false);

  useEffect(() => {
    const setup = async () => {
      try {
        await initDatabase();
        
        // Check for existing identity
        const db = await getDBConnection();
        const result = await db.executeSql('SELECT * FROM users LIMIT 1');
        
        if (result[0].rows.length > 0) {
          setHasAccount(true);
        }
      } catch (error) {
        console.error("Initialization Error:", error);
      }
      
      // Keep splash for 3 seconds
      setTimeout(() => setIsReady(true), 3000);
    };
    
    setup();
  }, []);

  if (!isReady) {
    return <SplashScreen onFinish={() => setIsReady(true)} />;
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        
        {/* Auth Flow */}
        {!hasAccount && (
          <Stack.Screen name="Register" component={RegisterScreen} />
        )}
        
        {/* Main App Flow */}
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen name="Discovery" component={DiscoveryScreen} />
        <Stack.Screen name="Connection" component={ConnectionScreen} />
        <Stack.Screen name="Chat" component={ChatScreen} />

      </Stack.Navigator>
    </NavigationContainer>
  );
}