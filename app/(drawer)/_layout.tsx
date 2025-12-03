import 'react-native-gesture-handler';
import { Drawer } from 'expo-router/drawer';
import CustomDrawerContent from './drawer';
import { useNotifications } from '@/hooks/useNotifications';

const DrawerLayout = () => {
  useNotifications();
  return (
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
  );
};

export default DrawerLayout;
