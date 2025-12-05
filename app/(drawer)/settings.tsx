
import { Fonts } from '@/constants/Fonts';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Switch,
  Button,
  Alert,
  Platform
} from 'react-native';
import { useTheme } from '../../hooks/useTheme';
import * as Notifications from 'expo-notifications';
import Slider from '@react-native-community/slider';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useNavigation } from '@react-navigation/native';
import { DrawerActions } from '@react-navigation/native';
import * as MediaLibrary from 'expo-media-library';
import AsyncStorage from '@react-native-async-storage/async-storage';

async function schedulePushNotification() {
    await Notifications.scheduleNotificationAsync({
      content: {
        title: "You've got mail! 📬",
        body: 'Here is the notification body',
        data: { data: 'goes here' },
      },
      trigger: { seconds: 2 },
    });
  }

const SettingsScreen = () => {
  const { theme, isDark, toggleTheme } = useTheme();
  const navigation = useNavigation();
  const [settings, setSettings] = useState({
    isYouTubeMusicEnabled: false,
    isBrowserNotificationsEnabled: false,
    isNewReleasesEnabled: true,
    isPlaylistUpdatesEnabled: false,
    isAutoPlayEnabled: true,
    isMonoAudioEnabled: false,
    isVolumeNormalizationEnabled: false,
    isGaplessPlaybackEnabled: true,
    isAutomixEnabled: true,
    crossfadeValue: 0,
    audioBalanceValue: 0.5,
    wifiAudioQuality: 'Automatic',
    cellularAudioQuality: 'Automatic',
    wifiVideoQuality: 'Automatic',
    cellularVideoQuality: 'Automatic',
  });

  const handleSettingChange = (key, value) => {
    setSettings(prevSettings => ({
      ...prevSettings,
      [key]: value,
    }));
  };

  const openDrawer = () => {
    navigation.dispatch(DrawerActions.openDrawer());
  };

  const exportSettings = async () => {
    try {
      const settingsString = JSON.stringify(settings, null, 2);
      const fileUri = FileSystem.cacheDirectory + 'settings.json';
      await FileSystem.writeAsStringAsync(fileUri, settingsString);
      await Sharing.shareAsync(fileUri, { mimeType: 'application/json', dialogTitle: 'Export Settings' });
    } catch (error) {
      console.error(error);
      Alert.alert('Error', 'Failed to export settings.');
    }
  };

  const importSettings = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/json',
      });
        if (result.assets && result.assets.length > 0) {
        const settingsString = await FileSystem.readAsStringAsync(result.assets[0].uri);
        const importedSettings = JSON.parse(settingsString);
        setSettings(importedSettings);
        Alert.alert('Success', 'Settings imported successfully.');
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to import settings.');
    }
  };

  const scanForMusic = async () => {
    const { status } = await MediaLibrary.requestPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert('Permission required', 'Please grant permission to access your media library to scan for music.');
      return;
    }

    const assets = await MediaLibrary.getAssetsAsync({
        mediaType: 'audio',
        first: 1000,
    });


    if (assets.assets.length > 0) {
      await AsyncStorage.setItem('downloaded_music', JSON.stringify(assets.assets));
      Alert.alert('Scan complete', `Found ${assets.assets.length} music files.`, [
        { text: 'OK', onPress: () => navigation.navigate('downloads' as never) },
      ]);
    } else {
      Alert.alert('No music found', 'Could not find any music files on your device.');
    }
  };

  const audioQualityOptions = ['Automatic', 'Very High', 'High', 'Standard', 'Low'];
  const videoQualityOptions = ['Automatic', 'Very High', 'High', 'Standard', 'Low'];

  const styles = getStyles(theme);

  return (
    <SafeAreaView style={styles.container}>
      <TouchableOpacity style={styles.drawerButton} onPress={openDrawer}>
        <Ionicons name="menu" size={32} color={theme.text} />
      </TouchableOpacity>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.contentContainer}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Settings</Text>
        </View>

        {/* Import/Export Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Manage Settings</Text>
          <View style={styles.card}>
            <TouchableOpacity style={styles.button} onPress={importSettings}>
                <Text style={styles.buttonText}>Import Settings</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.button} onPress={exportSettings}>
                <Text style={styles.buttonText}>Export Settings</Text>
            </TouchableOpacity>
          </View>
        </View>
        
        {/* Streaming Services Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Streaming Services</Text>
          <Text style={styles.sectionDescription}>Connect and manage your music services.</Text>
          <View style={styles.card}>
            <View style={styles.cardLeft}>
              <Ionicons name="logo-youtube" size={24} color="red" />
              <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>YouTube Music</Text>
                <Text style={styles.cardDescription}>Sync your library and playlists from YouTube Music.</Text>
              </View>
            </View>
            <Switch
              value={settings.isYouTubeMusicEnabled}
              onValueChange={value => handleSettingChange('isYouTubeMusicEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isYouTubeMusicEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Notifications Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Notifications</Text>
          <Text style={styles.sectionDescription}>Manage how you receive notifications.</Text>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Enable Notifications</Text>
              <Text style={styles.cardDescription}>Enable push notifications</Text>
            </View>
            <Switch
              value={settings.isBrowserNotificationsEnabled}
              onValueChange={value => handleSettingChange('isBrowserNotificationsEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isBrowserNotificationsEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>New Releases</Text>
              <Text style={styles.cardDescription}>Get notified about new music from artists you follow.</Text>
            </View>
            <Switch
              value={settings.isNewReleasesEnabled}
              onValueChange={value => handleSettingChange('isNewReleasesEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isNewReleasesEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Playlist Updates</Text>
              <Text style={styles.cardDescription}>Get notified when playlists you follow are updated.</Text>
            </View>
            <Switch
              value={settings.isPlaylistUpdatesEnabled}
              onValueChange={value => handleSettingChange('isPlaylistUpdatesEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isPlaylistUpdatesEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
            <Button
                title="Test Notification"
                onPress={async () => {
                await schedulePushNotification();
                }}
            />
        </View>

        {/* Listening Controls Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Listening Controls</Text>
          <Text style={styles.sectionDescription}>Customize your listening experience.</Text>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Auto Play</Text>
              <Text style={styles.cardDescription}>Automatically play similar songs when your music ends.</Text>
            </View>
            <Switch
              value={settings.isAutoPlayEnabled}
              onValueChange={value => handleSettingChange('isAutoPlayEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isAutoPlayEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Mono Audio</Text>
              <Text style={styles.cardDescription}>Makes the left and right speakers play the same audio.</Text>
            </View>
            <Switch
              value={settings.isMonoAudioEnabled}
              onValueChange={value => handleSettingChange('isMonoAudioEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isMonoAudioEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Audio Balance</Text>
              <Text style={styles.cardDescription}>Adjust the audio output between left and right channels.</Text>
            </View>
            <View style={styles.sliderContainer}>
                <Text style={styles.sliderLabel}>L</Text>
                <Slider
                    style={styles.slider}
                    minimumValue={0}
                    maximumValue={1}
                    value={settings.audioBalanceValue}
                    onValueChange={value => handleSettingChange('audioBalanceValue', value)}
                    minimumTrackTintColor={theme.primary}
                    maximumTrackTintColor={theme.sliderTrack}
                    thumbTintColor={theme.text}
                />
                <Text style={styles.sliderLabel}>R</Text>
            </View>
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Equaliser</Text>
              <Text style={styles.cardDescription}>Fine-tune your audio with presets or custom settings.</Text>
            </View>
            <TouchableOpacity style={styles.button}>
              <Text style={styles.buttonText}>Configure</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Volume Normalisation</Text>
              <Text style={styles.cardDescription}>Set the same volume level for all tracks.</Text>
            </View>
            <Switch
              value={settings.isVolumeNormalizationEnabled}
              onValueChange={value => handleSettingChange('isVolumeNormalizationEnabled', value)}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={settings.isVolumeNormalizationEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Playback Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Playback</Text>
          <Text style={styles.sectionDescription}>Adjust your streaming quality and transitions.</Text>
          <View style={styles.subSection}>
            <View style={styles.subSectionHeader}>
              <MaterialCommunityIcons name="music-note" size={20} color={theme.text} />
              <Text style={styles.subSectionTitle}>Audio Quality</Text>
            </View>
            <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <Ionicons name="wifi" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Wi-Fi Streaming</Text>
                </View>
                {audioQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => handleSettingChange('wifiAudioQuality', option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, settings.wifiAudioQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
            <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <MaterialCommunityIcons name="signal" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Cellular Streaming</Text>
                </View>
                {audioQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => handleSettingChange('cellularAudioQuality', option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, settings.cellularAudioQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
          </View>
          <View style={styles.subSection}>
            <View style={styles.subSectionHeader}>
              <MaterialCommunityIcons name="video" size={20} color={theme.text} />
              <Text style={styles.subSectionTitle}>Video Quality</Text>
            </View>
             <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <Ionicons name="wifi" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Wi-Fi Streaming</Text>
                </View>
                {videoQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => handleSettingChange('wifiVideoQuality', option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, settings.wifiVideoQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
            <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <MaterialCommunityIcons name="signal" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Cellular Streaming</Text>
                </View>
                {videoQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => handleSettingChange('cellularVideoQuality', option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, settings.cellularVideoQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
          </View>
        </View>

        {/* Track Transitions Section */}
        <View style={styles.section}>
            <View style={styles.subSectionHeader}>
                <MaterialCommunityIcons name="link-variant" size={20} color={theme.text} />
                <Text style={styles.subSectionTitle}>Track Transitions</Text>
            </View>
            <View style={styles.card}>
                <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>Gapless Playback</Text>
                <Text style={styles.cardDescription}>Allow gapless playback between songs.</Text>
                </View>
                <Switch
                value={settings.isGaplessPlaybackEnabled}
                onValueChange={value => handleSettingChange('isGaplessPlaybackEnabled', value)}
                trackColor={{ false: '#3e3e3e', true: theme.primary }}
                thumbColor={settings.isGaplessPlaybackEnabled ? theme.primary : '#f4f3f4'}
                />
            </View>
            <View style={styles.card}>
                <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>Automix</Text>
                <Text style={styles.cardDescription}>Allow smooth transitions between songs.</Text>
                </View>
                <Switch
                value={settings.isAutomixEnabled}
                onValueChange={value => handleSettingChange('isAutomixEnabled', value)}
                trackColor={{ false: '#3e3e3e', true: theme.primary }}
                thumbColor={settings.isAutomixEnabled ? theme.primary : '#f4f3f4'}
                />
            </View>
            <View style={styles.card}>
                <View style={styles.cardContent}>
                    <Text style={styles.cardTitle}>Crossfade</Text>
                    <Text style={styles.cardDescription}>Fade out the current song as the next one fades in.</Text>
                </View>
                <View style={styles.sliderContainer}>
                    <Slider
                        style={styles.slider}
                        minimumValue={0}
                        maximumValue={12}
                        step={1}
                        value={settings.crossfadeValue}
                        onValueChange={value => handleSettingChange('crossfadeValue', value)}
                        minimumTrackTintColor={theme.primary}
                        maximumTrackTintColor={theme.sliderTrack}
                        thumbTintColor={theme.text}
                    />
                    <Text style={styles.sliderLabel}>{`${settings.crossfadeValue}s`}</Text>
                </View>
            </View>
        </View>

        {/* Local Files Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Local Files</Text>
          <Text style={styles.sectionDescription}>Scan your device for local music files.</Text>
          <View style={styles.card}>
            <View style={styles.cardLeft}>
              <MaterialCommunityIcons name="folder-music" size={24} color={theme.text} />
              <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>Scan for music</Text>
                <Text style={styles.cardDescription}>Select folders on your device to scan for music</Text>
              </View>
            </View>
            <TouchableOpacity style={styles.button} onPress={scanForMusic}>
              <Text style={styles.buttonText}>Scan</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Appearance Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Appearance</Text>
          <Text style={styles.sectionDescription}>Customize the look and feel of the app.</Text>
          <View style={styles.card}>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>Dark Mode</Text>
              <Text style={styles.cardDescription}>Enjoy the app in a darker theme.</Text>
            </View>
            <Switch
              value={isDark}
              onValueChange={toggleTheme}
              trackColor={{ false: '#c2ffc2', true: theme.primary }}
              thumbColor={isDark ? theme.primary : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Developer Support Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Developer Support</Text>
          <Text style={styles.sectionDescription}>For any queries or support, please reach out to our developer.</Text>
          <View style={styles.card}>
            <Ionicons name="mail-outline" size={24} color={theme.text} />
            <Text style={styles.emailText}>zohaibbaig144@gmail.com</Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

export default SettingsScreen;

const getStyles = (theme: any) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.background,
  },
  drawerButton: {
    position: 'absolute',
    top: 40,
    left: 20,
    zIndex: 1,
  },
  contentContainer: {
    paddingHorizontal: 16,
  },
  header: {
    paddingVertical: 24,
  },
  headerTitle: {
    color: theme.text,
    fontFamily: Fonts.bold,
    fontSize: 32,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    color: theme.text,
    fontFamily: Fonts.bold,
    fontSize: 22,
    marginBottom: 4,
  },
  sectionDescription: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 14,
    marginBottom: 16,
  },
  card: {
    backgroundColor: theme.card,
    borderRadius: 12,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
    gap: 16,
  },
  cardLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    flex: 1,
  },
  cardContent: {
    flex: 1,
  },
  cardTitle: {
    color: theme.text,
    fontFamily: Fonts.bold,
    fontSize: 16,
  },
  cardDescription: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 14,
    marginTop: 2,
  },
  button: {
    backgroundColor: theme.button,
    borderRadius: 8,
    paddingVertical: 8,
    paddingHorizontal: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  buttonText: {
    color: theme.text,
    fontFamily: Fonts.bold,
  },
  sliderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  slider: {
    flex: 1,
  },
  sliderLabel: {
    color: theme.text,
    fontFamily: Fonts.regular,
  },
  subSection: {
      marginBottom: 24,
  },
  subSectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 16,
  },
  subSectionTitle: {
    color: theme.text,
    fontFamily: Fonts.bold,
    fontSize: 18,
  },
  streamingOptions: {
      backgroundColor: theme.card,
      borderRadius: 12,
      padding: 16,
      marginBottom: 12,
  },
  streamingHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
      marginBottom: 12,
  },
  streamingTitle: {
    color: theme.text,
    fontFamily: Fonts.regular,
  },
  radioOption: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingVertical: 10,
  },
  radioLabel: {
      color: theme.text,
      fontFamily: Fonts.regular,
  },
  radio: {
      width: 20,
      height: 20,
      borderRadius: 10,
      borderWidth: 2,
      borderColor: theme.text,
  },
  radioSelected: {
      backgroundColor: theme.text,
  },
  emailText: {
    color: theme.text,
    fontFamily: Fonts.regular,
    marginLeft: 16,
  },
});
