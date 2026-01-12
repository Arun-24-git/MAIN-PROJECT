import uuid from 'react-native-uuid';
import { RSA } from 'react-native-rsa-native';
export const generateUserId = () => uuid.v4().toString();
export const generateSecureKeyPair = async () => {
  const keys = await RSA.generateKeys(2048);
  return { publicKey: keys.public, privateKey: keys.private };
};