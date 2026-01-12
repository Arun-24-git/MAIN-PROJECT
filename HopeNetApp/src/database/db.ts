import SQLite from 'react-native-sqlite-storage';

// Enable Promise support for async/await
SQLite.enablePromise(true);

export const getDBConnection = async () => {
  return SQLite.openDatabase({ 
    name: 'hopenet.db', 
    location: 'default' 
  });
};

export const initDatabase = async () => {
  try {
    const db = await getDBConnection();
    
    /**
     * UPDATED SCHEMA:
     * - Replaced 'username' with 'phone_number'
     * - Set VARCHAR(15) for international phone number support
     * - Kept RSA Key fields and timestamps as per design
     */
    await db.executeSql(`
      CREATE TABLE IF NOT EXISTS users (
        user_id TEXT PRIMARY KEY NOT NULL,
        phone_number VARCHAR(15) NOT NULL, 
        public_key VARCHAR(1024) NOT NULL,
        private_key VARCHAR(8192) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);
    
    console.log("✅ HopeNet Database initialized with Phone Number schema");
  } catch (error) {
    console.error("❌ Database Init Error:", error);
  }
};