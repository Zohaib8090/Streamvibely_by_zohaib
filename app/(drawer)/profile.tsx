
import { useState, useEffect } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Image, Alert, TouchableOpacity } from 'react-native';
import { auth } from '@/firebase';
import { updateProfile, sendPasswordResetEmail } from 'firebase/auth';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { DrawerActions } from '@react-navigation/native';

const ProfileScreen = () => {
  const [displayName, setDisplayName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const user = auth.currentUser;
  const navigation = useNavigation();

  useEffect(() => {
    if (user) {
      setDisplayName(user.displayName || '');
      setAvatarUrl(user.photoURL || '');
    }
  }, [user]);

  const handleUpdateProfile = async () => {
    if (user) {
      try {
        await updateProfile(user, { displayName, photoURL: avatarUrl });
        Alert.alert('Success', 'Profile updated successfully!');
      } catch (error: any) {
        Alert.alert('Error', error.message);
      }
    }
  };

  const handleChangePassword = () => {
    if (user && user.email) {
      sendPasswordResetEmail(auth, user.email)
        .then(() => {
          Alert.alert('Password Reset', 'A password reset email has been sent to your email address.');
        })
        .catch((error) => {
          Alert.alert('Error', error.message);
        });
    }
  };

  const openDrawer = () => {
    navigation.dispatch(DrawerActions.openDrawer());
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.drawerButton} onPress={openDrawer}>
        <Ionicons name="menu" size={32} color={Colors.dark.text} />
      </TouchableOpacity>
      <View style={styles.header}>
        <Image source={{ uri: avatarUrl || 'https://via.placeholder.com/150' }} style={styles.avatar} />
        <Text style={styles.headerText}>{user?.displayName}</Text>
        <Text style={styles.emailText}>{user?.email}</Text>
      </View>
      <View style={styles.form}>
        <Text style={styles.label}>Display Name</Text>
        <TextInput
          style={styles.input}
          value={displayName}
          onChangeText={setDisplayName}
        />
        <Text style={styles.label}>Avatar URL</Text>
        <TextInput
          style={styles.input}
          value={avatarUrl}
          onChangeText={setAvatarUrl}
        />
        <View style={styles.buttonContainer}>
          <Button title="Update Profile" onPress={handleUpdateProfile} color={Colors.dark.primary} />
          <Button title="Change Password" onPress={handleChangePassword} color={Colors.dark.primary} />
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
    padding: 20,
  },
  drawerButton: {
    position: 'absolute',
    top: 40,
    left: 20,
    zIndex: 1,
  },
  header: {
    alignItems: 'center',
    marginBottom: 20,
  },
  avatar: {
    width: 100,
    height: 100,
    borderRadius: 50,
    marginBottom: 10,
  },
  headerText: {
    color: Colors.dark.text,
    fontSize: 24,
    fontFamily: Fonts.bold,
  },
  emailText: {
    color: Colors.dark.text,
    fontSize: 16,
    fontFamily: Fonts.regular,
  },
  form: {
    width: '100%',
  },
  label: {
    color: Colors.dark.text,
    fontSize: 16,
    fontFamily: Fonts.regular,
    marginBottom: 5,
  },
  input: {
    backgroundColor: Colors.dark.card,
    color: Colors.dark.text,
    padding: 10,
    borderRadius: 5,
    marginBottom: 15,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
});

export default ProfileScreen;
