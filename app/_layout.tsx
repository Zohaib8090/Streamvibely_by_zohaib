
import React, { useEffect, useState } from 'react';
import { Slot, useRouter, useSegments, useRootNavigationState } from 'expo-router';
import { onAuthStateChanged, User } from 'firebase/auth';
import { auth } from '../firebase';
import { SplashScreen } from 'expo-router';

SplashScreen.preventAutoHideAsync();

const RootLayout = () => {
  const [user, setUser] = useState<User | null>(null);
  const [authInitialized, setAuthInitialized] = useState(false);
  const segments = useSegments();
  const router = useRouter();
  const navigationState = useRootNavigationState();

  // Effect for checking authentication state
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      if (!authInitialized) {
        setAuthInitialized(true);
      }
    });
    return () => unsubscribe();
  }, []);

  // Effect for navigation
  useEffect(() => {
    if (!navigationState?.key || !authInitialized) {
      return;
    }

    const inAuthGroup = segments[0] === '(auth)';

    if (user && inAuthGroup) {
      // Logged in, but on auth screen -> redirect to app
      router.replace('/(tabs)');
    } else if (!user && !inAuthGroup) {
      // Not logged in and not in auth group -> redirect to login.
      router.replace('/(auth)/login');
    }
  }, [user, authInitialized, segments, router, navigationState]);

  // Hide splash screen once auth is initialized and navigation is ready
  useEffect(() => {
    if (authInitialized && navigationState?.key) {
      SplashScreen.hideAsync();
    }
  }, [authInitialized, navigationState]);

  // Render a loading indicator or null until everything is ready
  if (!authInitialized || !navigationState?.key) {
    return null;
  }

  return <Slot />;
};

export default RootLayout;
