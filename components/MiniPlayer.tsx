
import React from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { usePlayer } from '../contexts/PlayerContext';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';

const MiniPlayer = () => {
  const { currentTrack, isPlaying, pauseTrack, resumeTrack, openFullPlayer } = usePlayer();

  if (!currentTrack) {
    return null;
  }

  return (
    <TouchableOpacity onPress={openFullPlayer}>
      <View style={styles.container}>
        <Image source={{ uri: currentTrack.image }} style={styles.albumArt} />
        <View style={styles.trackInfo}>
          <Text style={styles.title}>{currentTrack.title}</Text>
          <Text style={styles.artist}>{currentTrack.artist}</Text>
        </View>
        <TouchableOpacity onPress={isPlaying ? pauseTrack : resumeTrack} style={styles.playButton}>
          <Ionicons name={isPlaying ? 'pause' : 'play'} size={24} color={Colors.dark.text} />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#2C2C2E',
    padding: 8,
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
  },
  albumArt: {
    width: 40,
    height: 40,
    borderRadius: 4,
  },
  trackInfo: {
    flex: 1,
    marginLeft: 10,
  },
  title: {
    color: Colors.dark.text,
    fontFamily: Fonts.bold,
    fontSize: 14,
  },
  artist: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 12,
  },
  playButton: {
    padding: 10,
  },
});

export default MiniPlayer;
