
import { DrawerActions } from '@react-navigation/native';
import { Link, useNavigation } from 'expo-router';
import { Tabs } from 'expo-router';
import React from 'react';
import { TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../../hooks/useTheme';

const TabsLayout = () => {
  const navigation = useNavigation();
  const { theme } = useTheme();

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: theme.text,
        tabBarStyle: {
          backgroundColor: theme.background,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ color }) => <Ionicons name="home" size={24} color={color} />,
          headerLeft: () => (
            <TouchableOpacity
              onPress={() => navigation.dispatch(DrawerActions.openDrawer())}
              style={{ marginLeft: 15 }}
            >
              <Ionicons name="menu" size={24} color={theme.text} />
            </TouchableOpacity>
          ),
          headerRight: () => (
            <Link href="/(drawer)/search" asChild>
              <TouchableOpacity style={{ marginRight: 15 }}>
                <Ionicons name="search" size={24} color={theme.text} />
              </TouchableOpacity>
            </Link>
          ),
          headerStyle: {
            backgroundColor: theme.background,
          },
          headerTintColor: theme.text,
        }}
      />
      <Tabs.Screen
        name="search"
        options={{
          headerShown: false,
          title: 'Search',
          tabBarIcon: ({ color }) => <Ionicons name="search" size={24} color={color} />,
        }}
      />
      <Tabs.Screen
        name="library"
        options={{
          headerShown: false,
          title: 'Library',
          tabBarIcon: ({ color }) => <Ionicons name="library" size={24} color={color} />,
        }}
      />
      <Tabs.Screen
        name="create-playlist"
        options={{
          headerShown: false,
          title: 'Create Playlist',
          tabBarIcon: ({ color }) => <Ionicons name="add-circle" size={24} color={color} />,
        }}
      />
    </Tabs>
  );
};

export default TabsLayout;
