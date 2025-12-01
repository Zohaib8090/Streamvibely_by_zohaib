
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
} from 'react-native';
import { useTheme } from '../../hooks/useTheme';

const SettingsScreen = () => {
  const { theme, isDark, toggleTheme } = useTheme();
  const [isYouTubeMusicEnabled, setIsYouTubeMusicEnabled] = useState(false);
  const [isNewReleasesEnabled, setIsNewReleasesEnabled] = useState(true);
  const [isPlaylistUpdatesEnabled, setIsPlaylistUpdatesEnabled] = useState(false);
  const [isAutoPlayEnabled, setIsAutoPlayEnabled] = useState(true);
  const [isMonoAudioEnabled, setIsMonoAudioEnabled] = useState(false);
  const [isVolumeNormalizationEnabled, setIsVolumeNormalizationEnabled] = useState(false);
  const [isGaplessPlaybackEnabled, setIsGaplessPlaybackEnabled] = useState(true);
  const [isAutomixEnabled, setIsAutomixEnabled] = useState(true);

  const [wifiAudioQuality, setWifiAudioQuality] = useState('Automatic');
  const [cellularAudioQuality, setCellularAudioQuality] = useState('Automatic');
  const [wifiVideoQuality, setWifiVideoQuality] = useState('Automatic');
  const [cellularVideoQuality, setCellularVideoQuality] = useState('Automatic');

  const audioQualityOptions = ['Automatic', 'Very High', 'High', 'Standard', 'Low'];
  const videoQualityOptions = ['Automatic', 'Very High', 'High', 'Standard', 'Low'];

  const styles = getStyles(theme);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.contentContainer}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Settings</Text>
        </View>

        {/* Streaming Services Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Streaming Services</Text>
          <Text style={styles.sectionDescription}>Connect and manage your music services.</Text>
          <View style={styles.card}>
            <View style={styles.cardLeft}>
              <Ionicons name="logo-youtube" size={24} color="red" />
              <View>
                <Text style={styles.cardTitle}>YouTube Music</Text>
                <Text style={styles.cardDescription}>Sync your library and playlists from YouTube Music.</Text>
              </View>
            </View>
            <Switch
              value={isYouTubeMusicEnabled}
              onValueChange={setIsYouTubeMusicEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isYouTubeMusicEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Notifications Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Notifications</Text>
          <Text style={styles.sectionDescription}>Manage how you receive notifications.</Text>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Browser Notifications</Text>
              <Text style={styles.cardDescription}>Enable browser notifications</Text>
            </View>
            <TouchableOpacity style={styles.button}>
              <Ionicons name="notifications-outline" size={16} color={theme.text} />
              <Text style={styles.buttonText}>Enable</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>New Releases</Text>
              <Text style={styles.cardDescription}>Get notified about new music from artists you follow.</Text>
            </View>
            <Switch
              value={isNewReleasesEnabled}
              onValueChange={setIsNewReleasesEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isNewReleasesEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Playlist Updates</Text>
              <Text style={styles.cardDescription}>Get notified when playlists you follow are updated.</Text>
            </View>
            <Switch
              value={isPlaylistUpdatesEnabled}
              onValueChange={setIsPlaylistUpdatesEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isPlaylistUpdatesEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
        </View>

        {/* Listening Controls Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Listening Controls</Text>
          <Text style={styles.sectionDescription}>Customize your listening experience.</Text>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Auto Play</Text>
              <Text style={styles.cardDescription}>Automatically play similar songs when your music ends.</Text>
            </View>
            <Switch
              value={isAutoPlayEnabled}
              onValueChange={setIsAutoPlayEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isAutoPlayEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Mono Audio</Text>
              <Text style={styles.cardDescription}>Makes the left and right speakers play the same audio.</Text>
            </View>
            <Switch
              value={isMonoAudioEnabled}
              onValueChange={setIsMonoAudioEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isMonoAudioEnabled ? theme.primary : '#f4f3f4'}
            />
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Audio Balance</Text>
              <Text style={styles.cardDescription}>Adjust the audio output between left and right channels.</Text>
            </View>
            {/* Slider placeholder */}
            <View style={styles.sliderContainer}>
                <Text style={styles.sliderLabel}>L</Text>
                <View style={styles.sliderTrack}><View style={styles.sliderThumb} /></View>
                <Text style={styles.sliderLabel}>R</Text>
            </View>
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Equaliser</Text>
              <Text style={styles.cardDescription}>Fine-tune your audio with presets or custom settings.</Text>
            </View>
            <TouchableOpacity style={styles.button}>
              <Text style={styles.buttonText}>Configure</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.card}>
            <View>
              <Text style={styles.cardTitle}>Volume Normalisation</Text>
              <Text style={styles.cardDescription}>Set the same volume level for all tracks.</Text>
            </View>
            <Switch
              value={isVolumeNormalizationEnabled}
              onValueChange={setIsVolumeNormalizationEnabled}
              trackColor={{ false: '#3e3e3e', true: theme.primary }}
              thumbColor={isVolumeNormalizationEnabled ? theme.primary : '#f4f3f4'}
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
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => setWifiAudioQuality(option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, wifiAudioQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
            <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <MaterialCommunityIcons name="signal" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Cellular Streaming</Text>
                </View>
                {audioQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => setCellularAudioQuality(option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, cellularAudioQuality === option && styles.radioSelected]} />
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
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => setWifiVideoQuality(option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, wifiVideoQuality === option && styles.radioSelected]} />
                    </TouchableOpacity>
                ))}
            </View>
            <View style={styles.streamingOptions}>
                <View style={styles.streamingHeader}>
                    <MaterialCommunityIcons name="signal" size={16} color={theme.text} />
                    <Text style={styles.streamingTitle}>Cellular Streaming</Text>
                </View>
                {videoQualityOptions.map(option => (
                    <TouchableOpacity key={option} style={styles.radioOption} onPress={() => setCellularVideoQuality(option)}>
                        <Text style={styles.radioLabel}>{option}</Text>
                        <View style={[styles.radio, cellularVideoQuality === option && styles.radioSelected]} />
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
                <View>
                <Text style={styles.cardTitle}>Gapless Playback</Text>
                <Text style={styles.cardDescription}>Allow gapless playback between songs.</Text>
                </View>
                <Switch
                value={isGaplessPlaybackEnabled}
                onValueChange={setIsGaplessPlaybackEnabled}
                trackColor={{ false: '#3e3e3e', true: theme.primary }}
                thumbColor={isGaplessPlaybackEnabled ? theme.primary : '#f4f3f4'}
                />
            </View>
            <View style={styles.card}>
                <View>
                <Text style={styles.cardTitle}>Automix</Text>
                <Text style={styles.cardDescription}>Allow smooth transitions between songs.</Text>
                </View>
                <Switch
                value={isAutomixEnabled}
                onValueChange={setIsAutomixEnabled}
                trackColor={{ false: '#3e3e3e', true: theme.primary }}
                thumbColor={isAutomixEnabled ? theme.primary : '#f4f3f4'}
                />
            </View>
            <View style={styles.card}>
                <View>
                    <Text style={styles.cardTitle}>Crossfade</Text>
                    <Text style={styles.cardDescription}>Fade out the current song as the next one fades in.</Text>
                </View>
                <View style={styles.sliderContainer}>
                    <View style={styles.sliderTrack}><View style={styles.sliderThumb} /></View>
                    <Text style={styles.sliderLabel}>0s</Text>
                </View>
            </View>
        </View>

        {/* Appearance Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Appearance</Text>
          <Text style={styles.sectionDescription}>Customize the look and feel of the app.</Text>
          <View style={styles.card}>
            <View>
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
  },
  cardLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
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
  },
  sliderTrack: {
    height: 4,
    width: 100,
    backgroundColor: theme.sliderTrack,
    borderRadius: 2,
    justifyContent: 'center',
  },
  sliderThumb: {
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: theme.text,
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
