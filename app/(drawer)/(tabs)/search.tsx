
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';

const YOUTUBE_API_KEY = 'AIzaSyBiuTKC3t93mq7ardC2_AVwD5CVhM2TFIc';

const SearchScreen = () => {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchHistory, setSearchHistory] = useState([
    { term: 'talwinder songs' },
    { term: 'ehasaas' },
    { term: 'katilana' },
    { term: 'kashish song' },
    { term: 'gaming hot' },
    { term: 'gaming hit' },
    { term: 'gaming' },
    { term: 'gaming music' },
    { term: 'boyna galava' },
    { term: 'vina gala song' },
    { term: 'nadaaniyan movie song' },
    { term: 'nadaniyaan song' },
  ]);

  const handleSearch = async () => {
    if (!searchQuery) return;

    setSearchHistory([{ term: searchQuery }, ...searchHistory]);

    try {
      const response = await fetch(
        `https://www.googleapis.com/youtube/v3/search?part=snippet&q=${searchQuery}&key=${YOUTUBE_API_KEY}`
      );
      const data = await response.json();
      console.log(data.items);
    } catch (error) {
      console.error(error);
    }
  };

  const handleMicSearch = () => {
    console.log('Mic search activated');
  };

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
          <TouchableOpacity onPress={handleMicSearch}>
            <Ionicons name="mic-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="options-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
        </View>
      </View>
      <ScrollView>
        {searchHistory.map((item, index) => (
          <View key={index} style={styles.historyItem}>
            <MaterialCommunityIcons name="history" size={24} color="#8E8E93" />
            <Text style={styles.historyText}>{item.term}</Text>
            <TouchableOpacity>
              <MaterialCommunityIcons name="arrow-top-left" size={24} color="#8E8E93" />
            </TouchableOpacity>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
};

export default SearchScreen;

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
  historyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    gap: 16,
  },
  historyText: {
    flex: 1,
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
    fontSize: 16,
  },
});
