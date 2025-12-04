
import 'react-native-gesture-handler';
import { useAuth } from '../hooks/useAuth';
import { Slot, useRouter, useSegments } from 'expo-router';
import { useEffect } from 'react';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { ThemeProvider } from '../hooks/useTheme';
import { PlayerProvider } from '../contexts/PlayerContext';

const InitialLayout = () => {
  const { user, initialized } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (!initialized) return;

    const inAuthGroup = segments[0] === '(auth)';

    if (user && !inAuthGroup) {
      router.replace('/(drawer)/(tabs)');
    } else if (!user) {
      router.replace('/(auth)/login');
    }
  }, [user, initialized]);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <ThemeProvider>
        <PlayerProvider>
          <Slot />
        </PlayerProvider>
      </ThemeProvider>
    </GestureHandlerRootView>
  );
};

export default InitialLayout;
