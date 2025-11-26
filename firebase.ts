
import { initializeApp } from "firebase/app";
import { getAuth, connectAuthEmulator, initializeAuth } from "firebase/auth";
import { getFirestore, connectFirestoreEmulator, enableIndexedDbPersistence } from "firebase/firestore";
import { getReactNativePersistence } from 'firebase/auth/react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const firebaseConfig = {
  apiKey: "AIzaSyDiAKXm9sOrjPmKb0qgY6h4jQ-f763NDZM",
  authDomain: "streamvibely.firebaseapp.com",
  projectId: "streamvibely",
  storageBucket: "streamvibely.firebasestorage.app",
  messagingSenderId: "731940880209",
  appId: "1:731940880209:web:43adce6688fd4245db9d70",
  measurementId: "G-9QCWFV3X71"
};

const app = initializeApp(firebaseConfig);
const auth = initializeAuth(app, {
  persistence: getReactNativePersistence(AsyncStorage)
});
const db = getFirestore(app);

if (__DEV__) {
  connectAuthEmulator(auth, "http://127.0.0.1:9099");
  connectFirestoreEmulator(db, '127.0.0.1', 8080);
}

enableIndexedDbPersistence(db).catch((err) => {
    if (err.code == 'failed-precondition') {
    } else if (err.code == 'unimplemented') {
    }
});

export { app, auth, db };
