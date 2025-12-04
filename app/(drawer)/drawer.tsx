import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { auth } from '@/firebase';
import {
  DrawerContentScrollView,
  DrawerItem
} from '@react-navigation/drawer';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { Text, View, StyleSheet } from 'react-native';

const CustomDrawerContent = (props: any) => {
  const router = useRouter();
  const handleSignOut = async () => {
    await auth.signOut();
    router.replace('/(auth)/login');
  };

  return (
    <DrawerContentScrollView {...props} style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerText}>Menu</Text>
      </View>
      <DrawerItem
        label="Home"
        onPress={() => router.push('/(drawer)/(tabs)')}
        labelStyle={styles.drawerItemLabel}
        icon={({ size }) => <Ionicons name="home-outline" size={size} color='#fff' />}
      />
      <DrawerItem
        label="View Profile"
        onPress={() => router.push('/(drawer)/profile')}
        labelStyle={styles.drawerItemLabel}
        icon={({ size }) => <Ionicons name="person-outline" size={size} color='#fff' />}
      />
      <DrawerItem
        label="What's new"
        onPress={() => {}}
        labelStyle={styles.drawerItemLabel}
        icon={({ size }) => <Ionicons name="notifications-outline" size={size} color='#fff' />}
      />
      <DrawerItem
        label="Settings"
        onPress={() => router.push('/(drawer)/settings')}
        labelStyle={styles.drawerItemLabel}
        icon={({ size }) => <Ionicons name="settings-outline" size={size} color='#fff' />}
      />
      <DrawerItem
        label="Logout"
        onPress={handleSignOut}
        labelStyle={styles.drawerItemLabel}
        icon={({ size }) => <Ionicons name="log-out-outline" size={size} color='#fff' />}
      />
    </DrawerContentScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
  },
  header: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: Colors.dark.text,
  },
  headerText: {
    color: Colors.dark.text,
    fontSize: 20,
    fontFamily: Fonts.bold,
  },
  drawerItemLabel: {
    color: '#fff',
    fontFamily: Fonts.regular,
  },
});

export default CustomDrawerContent;
