
import 'react-native-gesture-handler';
import { Drawer } from 'expo-router/drawer';
import CustomDrawerContent from './drawer';
import { useNotifications } from '@/hooks/useNotifications';
import { usePlayer } from '@/contexts/PlayerContext';
import MiniPlayer from '@/components/MiniPlayer';
import FullPlayer from '@/components/FullPlayer';

const DrawerLayout = () => {
  useNotifications();
  const { playerState } = usePlayer();

  return (
    <>
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
      {playerState === 'mini' && <MiniPlayer />}
      {playerState === 'full' && <FullPlayer />}
    </>
  );
};

export default DrawerLayout;
