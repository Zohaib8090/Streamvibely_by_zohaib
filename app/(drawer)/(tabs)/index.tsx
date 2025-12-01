
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React from 'react';
import {
  Image,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

const playlists = [
  {
    title: 'Chill Vibes',
    subtitle: 'Relax and unwind with these m...',
    image: 'https://images.pexels.com/photos/1763075/pexels-photo-1763075.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2',
  },
  {
    title: 'Workout Beats',
    subtitle: 'High-energy tracks to power y...',
    image: 'https://images.pexels.com/photos/3757954/pexels-photo-3757954.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2',
  },
  {
    title: 'Summer Hits',
    subtitle: 'The hottest tracks of the se...',
    image: 'https://images.pexels.com/photos/1493226/pexels-photo-1493226.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2',
  },
  {
    title: 'Focus Flow',
    subtitle: 'Instrumental tracks to help y...',
    image: 'https://images.pexels.com/photos/3771835/pexels-photo-3771835.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2',
  },
];

const HomeScreen = () => {
  const router = useRouter();
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Personalized Recommendations</Text>
          <TouchableOpacity onPress={() => router.push('/(drawer)/search')}>
            <Ionicons name="search-outline" size={28} color={Colors.dark.text} />
          </TouchableOpacity>
        </View>
        <View style={styles.suggestionsCard}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
            <MaterialCommunityIcons name="star-four-points-outline" size={24} color="white" />
            <Text style={styles.suggestionsTitle}>Personalized Suggestions</Text>
          </View>
          <Text style={styles.suggestionsSubtitle}>
            Our AI will recommend songs based on your listening history. The more you listen, the
            better the recommendations.
          </T ext>
        </View>
        <TouchableOpacity style={styles.suggestionsButton}>
          <MaterialCommunityIcons name="star-four-points-outline" size={24} color="white" />
          <Text style={styles.suggestionsButtonText}>Get Fresh Recommendations</Text>
        </TouchableOpacity>
        <View style={styles.divider} />
        <Text style={styles.playlistsTitle}>Playlists</Text>
        <View style={styles.playlistsGrid}>
          {playlists.map((playlist, index) => (
            <View key={index} style={styles.playlistItem}>
              <Image source={{ uri: playlist.image }} style={styles.playlistImage} />
              <Text style={styles.playlistItemTitle}>{playlist.title}</Text>
              <Text style={styles.playlistItemSubtitle}>{playlist.subtitle}</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

export default HomeScreen;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
    paddingHorizontal: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 24,
    marginBottom: 16,
  },
  headerTitle: {
    fontSize: 28,
    fontFamily: Fonts.bold,
    color: Colors.dark.text,
  },
  suggestionsCard: {
    backgroundColor: '#1C1C1E',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  suggestionsTitle: {
    fontSize: 18,
    fontFamily: Fonts.bold,
    color: Colors.dark.text,
  },
  suggestionsSubtitle: {
    fontSize: 14,
    fontFamily: Fonts.regular,
    color: '#8E8E93',
    marginTop: 8,
  },
  suggestionsButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#3A3A3C',
    paddingVertical: 14,
    borderRadius: 12,
    gap: 10,
  },
  suggestionsButtonText: {
    fontSize: 16,
    fontFamily: Fonts.bold,
    color: Colors.dark.text,
  },
  divider: {
    height: 1,
    backgroundColor: '#2C2C2E',
    marginVertical: 24,
  },
  playlistsTitle: {
    fontSize: 22,
    fontFamily: Fonts.bold,
    color: Colors.dark.text,
    marginBottom: 16,
  },
  playlistsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  playlistItem: {
    width: '48%',
    marginBottom: 16,
  },
  playlistImage: {
    width: '100%',
    height: 150,
    borderRadius: 8,
  },
  playlistItemTitle: {
    fontSize: 16,
    fontFamily: Fonts.bold,
    color: Colors.dark.text,
    marginTop: 8,
  },
  playlistItemSubtitle: {
    fontSize: 12,
    fontFamily: Fonts.regular,
    color: '#8E8E93',
  },
});
