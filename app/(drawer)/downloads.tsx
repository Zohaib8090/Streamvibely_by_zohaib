
import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity, FlatList, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { useRouter } from 'expo-router';
import * as MediaLibrary from 'expo-media-library';
import { usePlayer } from '@/contexts/PlayerContext';

const DownloadsScreen = () => {
  const router = useRouter();
  const [musicFiles, setMusicFiles] = useState<MediaLibrary.Asset[]>([]);
  const { play } = usePlayer();

  useEffect(() => {
    (async () => {
      const { status } = await MediaLibrary.requestPermissionsAsync();
      if (status !== 'granted') {
        alert('Permission to access media library is required!');
        return;
      }

      const media = await MediaLibrary.getAssetsAsync({
        mediaType: 'audio',
      });
      setMusicFiles(media.assets);
    })();
  }, []);

  const renderItem = ({ item }: { item: MediaLibrary.Asset }) => (
    <TouchableOpacity style={styles.songItem} onPress={() => play(item)}>
      <Image source={{ uri: item.uri }} style={styles.albumArt} />
      <View style={styles.songInfo}>
        <Text style={styles.songTitle}>{item.filename}</Text>
        <Text style={styles.songArtist}>{item.albumId}</Text>
      </View>
      <TouchableOpacity onPress={() => play(item)}>
        <Ionicons name="play-circle" size={32} color={Colors.dark.text} />
      </TouchableOpacity>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
        <Text style={styles.title}>Downloads</Text>
        <TouchableOpacity>
          <Ionicons name="search" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
      </View>
      <FlatList
        data={musicFiles}
        renderItem={renderItem}
        keyExtractor={(item) => item.id}
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
  content: {
    paddingHorizontal: 16,
  },
  songItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.dark.tint,
  },
  albumArt: {
    width: 50,
    height: 50,
    borderRadius: 8,
    marginRight: 12,
  },
  songInfo: {
    flex: 1,
  },
  songTitle: {
    color: Colors.dark.text,
    fontSize: 16,
    fontFamily: Fonts.medium,
  },
  songArtist: {
    color: Colors.dark.text,
    fontSize: 14,
    fontFamily: Fonts.regular,
  },
});

export default DownloadsScreen;
