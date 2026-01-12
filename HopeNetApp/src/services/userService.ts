import { getDBConnection } from '../database/db';
import { generateUserId, generateSecureKeyPair } from '../utils/cryptoUtils';

/**
 * Service to register a new user using their phone number.
 * Fulfills User Story 1 (Offline Profile) and User Story 2 (Secure Identity).
 */
export const registerUser = async (phoneNumber: string) => {
  try {
    const db = await getDBConnection();
    
    // 1. Generate unique offline ID (UUID v4)
    const userId = generateUserId();
    
    // 2. Generate 2048-bit RSA keys for secure identity
    const { publicKey, privateKey } = await generateSecureKeyPair();

    // 3. Prepare the SQL query (Matching the new phone_number column)
    const query = `INSERT INTO users (user_id, phone_number, public_key, private_key) VALUES (?, ?, ?, ?)`;
    const params = [userId, phoneNumber, publicKey, privateKey];

    // 4. Execute transaction
    await db.executeSql(query, params);
    
    console.log(`✅ Registration successful for: ${phoneNumber}`);
    return userId;
    
  } catch (error) {
    console.error("❌ Service Error in registerUser:", error);
    throw error; // Let the RegisterScreen handle the alert message
  }
};