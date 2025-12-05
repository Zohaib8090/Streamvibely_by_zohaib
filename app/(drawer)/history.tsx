
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity, FlatList, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { useRouter } from 'expo-router';
import { playTrack, getHistory, clearHistory } from '@/utils/MusicPlayer';

const HistoryScreen = () => {
  const router = useRouter();
  const [history, setHistory] = useState([]);

  useEffect(() => {
    const loadHistory = async () => {
      const historyData = await getHistory();
      setHistory(historyData);
    };
    loadHistory();
  }, []);

  const handlePlayTrack = async (track) => {
    await playTrack(track);
    const historyData = await getHistory();
    setHistory(historyData);
  };

  const handleClearHistory = async () => {
    await clearHistory();
    setHistory([]);
  };

  const renderHistoryItem = ({ item }) => (
    <TouchableOpacity onPress={() => handlePlayTrack(item)}>
      <View style={styles.historyItem}>
        <Image source={{ uri: item.artwork }} style={styles.artwork} />
        <View style={styles.trackInfo}>
          <Text style={styles.trackTitle}>{item.title}</Text>
          <Text style={styles.trackArtist}>{item.artist}</Text>
        </View>
        <Ionicons name={item.type === 'youtube' ? 'logo-youtube' : 'musical-notes'} size={24} color={Colors.dark.text} />
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
        <Text style={styles.title}>History</Text>
        <TouchableOpacity onPress={handleClearHistory}>
          <Text style={styles.clearButton}>Clear</Text>
        </TouchableOpacity>
      </View>
      <FlatList
        data={history}
        renderItem={renderHistoryItem}
        keyExtractor={(item, index) => `${item.id}-${index}`}
        contentContainerStyle={styles.content}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.dark.tint,
  },
  title: {
    color: Colors.dark.text,
    fontSize: 20,
    fontFamily: Fonts.bold,
  },
  clearButton: {
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
    fontSize: 16,
  },
  content: {
    paddingHorizontal: 16,
  },
  historyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
  },
  artwork: {
    width: 50,
    height: 50,
    borderRadius: 4,
    marginRight: 12,
  },
  trackInfo: {
    flex: 1,
  },
  trackTitle: {
    color: Colors.dark.text,
    fontSize: 16,
    fontFamily: Fonts.regular,
  },
  trackArtist: {
    color: Colors.dark.text,
    fontSize: 14,
    fontFamily: Fonts.light,
  },
});

export default HistoryScreen;
