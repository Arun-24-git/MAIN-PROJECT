import React, { useState, useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { initDatabase, getDBConnection } from './src/database/db';

import SplashScreen from './src/screens/SplashScreen';
import RegisterScreen from './src/screens/RegisterScreen';
import HomeScreen from './src/screens/HomeScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  const [isReady, setIsReady] = useState(false);
  const [hasAccount, setHasAccount] = useState(false);

  useEffect(() => {
    const setup = async () => {
      await initDatabase();
      const db = await getDBConnection();
      const result = await db.executeSql('SELECT * FROM users');
      if (result[0].rows.length > 0) setHasAccount(true);
      
      // Keep splash for 3 seconds
      setTimeout(() => setIsReady(true), 3000);
    };
    setup();
  }, []);

  if (!isReady) return <SplashScreen onFinish={() => setIsReady(true)} />;

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {/* If user exists, go straight to Home, else show Register */}
        {!hasAccount ? (
          <Stack.Screen name="Register" component={RegisterScreen} />
        ) : null}
        <Stack.Screen name="Home" component={HomeScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}