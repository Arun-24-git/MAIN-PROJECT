import SQLite from 'react-native-sqlite-storage';

SQLite.enablePromise(true);

export const getDBConnection = async () => {
  return SQLite.openDatabase({ name: 'hopenet.db', location: 'default' });
};

export const initDatabase = async () => {
  try {
    const db = await getDBConnection();
    
    // User Table
    await db.executeSql(`
      CREATE TABLE IF NOT EXISTS users (
        user_id TEXT PRIMARY KEY NOT NULL,
        phone_number VARCHAR(15) NOT NULL, 
        public_key VARCHAR(1024) NOT NULL,
        private_key VARCHAR(8192) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );
    `);

    // --- NEW: Device Table (Image 3 Requirement) ---
    await db.executeSql(`
      CREATE TABLE IF NOT EXISTS devices (
        device_id TEXT PRIMARY KEY NOT NULL,
        user_id TEXT,
        last_seen TIMESTAMP NOT NULL,
        signal_strength INTEGER NULL,
        is_reachable INTEGER DEFAULT 0,
        FOREIGN KEY (user_id) REFERENCES users (user_id)
      );
    `);
    
    console.log("✅ Database and Device Tables Initialized");
  } catch (error) {
    console.error("❌ Database Init Error:", error);
  }
};