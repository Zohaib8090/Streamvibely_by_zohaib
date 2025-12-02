import { Drawer } from 'expo-router/drawer';
import CustomDrawerContent from './drawer';
import { ThemeProvider } from '@/hooks/useTheme';

const DrawerLayout = () => {
  return (
    <ThemeProvider>
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
    </ThemeProvider>
  );
};

export default DrawerLayout;
