
import React from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity, ScrollView, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { useRouter } from 'expo-router';
import { usePlayer } from '../../contexts/PlayerContext';

const HistoryScreen = () => {
  const router = useRouter();
  const { history, play } = usePlayer();

  const handlePlay = (track) => {
    play(track, history);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
        <Text style={styles.title}>History</Text>
        <View />
      </View>
      <ScrollView contentContainerStyle={styles.content}>
        {history.length > 0 ? (
          history.map((item) => (
            <TouchableOpacity key={item.id} style={styles.resultItem} onPress={() => handlePlay(item)}>
              <Image source={{ uri: item.image }} style={styles.resultImage} />
              <View style={styles.resultTextContainer}>
                <Text style={styles.resultTitle}>{item.title}</Text>
                <Text style={styles.resultArtist}>{item.artist}</Text>
              </View>
            </TouchableOpacity>
          ))
        ) : (
          <Text style={styles.emptyText}>No recently played songs.</Text>
        )}
      </ScrollView>
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
  emptyText: {
    color: Colors.dark.text,
    textAlign: 'center',
    marginTop: 50,
    fontFamily: Fonts.regular,
  },
  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    gap: 12,
  },
  resultImage: {
    width: 50,
    height: 50,
    borderRadius: 8,
  },
  resultTextContainer: {
    flex: 1,
  },
  resultTitle: {
    color: Colors.dark.text,
    fontFamily: Fonts.bold,
    fontSize: 16,
  },
  resultArtist: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 14,
  },
});

export default HistoryScreen;
