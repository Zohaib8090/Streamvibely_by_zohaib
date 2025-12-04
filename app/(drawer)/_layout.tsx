import 'react-native-gesture-handler';
import { Drawer } from 'expo-router/drawer';
import CustomDrawerContent from './drawer';
import { useNotifications } from '@/hooks/useNotifications';
import { PlayerProvider } from '../../contexts/PlayerContext';
import MiniPlayer from '../../components/MiniPlayer';
import { View } from 'react-native';

const DrawerLayout = () => {
  useNotifications();
  return (
    <PlayerProvider>
      <View style={{ flex: 1 }}>
        <Drawer
          drawerContent={(props) => <CustomDrawerContent {...props} />}
          screenOptions={({ route }) => ({
            headerShown: false,
            drawerActiveTintColor: '#fff',
            drawerInactiveTintColor: '#fff',
            drawerItemStyle:
              route.name === '(tabs)' || route.name === 'drawer'
                ? { display: 'none' }
                : {},
          })}
        />
        <MiniPlayer />
      </View>
    </PlayerProvider>
  );
};

export default DrawerLayout;
