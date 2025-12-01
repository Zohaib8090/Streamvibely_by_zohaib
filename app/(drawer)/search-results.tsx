
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';

const YOUTUBE_API_KEY = 'AIzaSyBiuTKC3t93mq7ardC2_AVwD5CVhM2TFIc';

const SearchResultsScreen = () => {
  const router = useRouter();
  const { query } = useLocalSearchParams();
  const [searchQuery, setSearchQuery] = useState(query as string);
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const searchTabs = ['YT MUSIC', 'LIBRARY'];
  const filterOptions = ['Songs', 'Artists', 'Videos', 'Community playlists', 'Episodes'];

  const handleSearch = async () => {
    if (!searchQuery) return;

    setLoading(true);
    try {
      const response = await fetch(
        `https://www.googleapis.com/youtube/v3/search?part=snippet&q=${searchQuery}&key=${YOUTUBE_API_KEY}`
      );
      const data = await response.json();
      setResults(data.items || []);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    handleSearch();
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
        <View style={styles.searchBar}>
          <TextInput
            placeholder="Search songs, artist..."
            placeholderTextColor="#8E8E93"
            style={styles.searchInput}
            value={searchQuery}
            onChangeText={setSearchQuery}
            onSubmitEditing={handleSearch}
          />
          <TouchableOpacity onPress={() => setSearchQuery('')}>
            <Ionicons name="close" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="mic-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="options-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.tabsContainer}>
        {searchTabs.map((tab) => (
          <TouchableOpacity key={tab} style={styles.tab}>
            <Text style={styles.tabText}>{tab}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filtersContainer}>
        {filterOptions.map((option) => (
          <TouchableOpacity key={option} style={styles.filterChip}>
            <Text style={styles.filterText}>{option}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading ? (
        <ActivityIndicator size="large" color={Colors.dark.text} style={{ flex: 1 }} />
      ) : (
        <ScrollView style={styles.resultsContainer}>
          {results.map((item) => (
            <View key={item.id.videoId} style={styles.resultItem}>
              <Image
                source={{ uri: item.snippet.thumbnails.default.url }}
                style={styles.resultThumbnail}
              />
              <View style={styles.resultInfo}>
                <Text style={styles.resultTitle}>{item.snippet.title}</Text>
                <Text style={styles.resultSubtitle}>{item.snippet.channelTitle}</Text>
              </View>
              <Ionicons name="ellipsis-vertical" size={24} color={Colors.dark.text} />
            </View>
          ))}
        </ScrollView>
      )}
    </SafeAreaView>
  );
};

export default SearchResultsScreen;

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.dark.background,
        paddingHorizontal: 16,
      },
      header: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 24,
        marginBottom: 16,
        gap: 12,
      },
      searchBar: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: '#1C1C1E',
        borderRadius: 12,
        paddingHorizontal: 12,
        gap: 12,
      },
      searchInput: {
        flex: 1,
        color: Colors.dark.text,
        fontFamily: Fonts.regular,
        fontSize: 16,
        paddingVertical: 10,
      },
      tabsContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginBottom: 16,
      },
      tab: {
        paddingVertical: 8,
        paddingHorizontal: 24,
      },
      tabText: {
        color: Colors.dark.text,
        fontFamily: Fonts.bold,
        fontSize: 16,
      },
      filtersContainer: {
        flexDirection: 'row',
        marginBottom: 16,
      },
      filterChip: {
        backgroundColor: '#2C2C2E',
        borderRadius: 16,
        paddingVertical: 8,
        paddingHorizontal: 16,
        marginRight: 8,
      },
      filterText: {
        color: Colors.dark.text,
        fontFamily: Fonts.regular,
      },
      resultsContainer: {
        flex: 1,
      },
      resultItem: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 16,
      },
      resultThumbnail: {
        width: 48,
        height: 48,
        borderRadius: 4,
      },
      resultInfo: {
        flex: 1,
        marginLeft: 16,
      },
      resultTitle: {
        color: Colors.dark.text,
        fontFamily: Fonts.regular,
        fontSize: 16,
      },
      resultSubtitle: {
        color: '#8E8E93',
        fontFamily: Fonts.regular,
        fontSize: 14,
      },
});
