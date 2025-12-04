
import React from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../hooks/useTheme';
import { Fonts } from '@/constants/Fonts';
import { usePlayer } from '../contexts/PlayerContext';

const MiniPlayer = () => {
  const { theme } = useTheme();
  const { currentTrack, isPlaying, pauseTrack, resumeTrack } = usePlayer();

  if (!currentTrack) {
    return null;
  }

  const handlePlayPause = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      resumeTrack();
    }
  };

  const styles = StyleSheet.create({
    container: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: theme.miniPlayer,
      padding: 8,
    },
    albumArt: {
      width: 48,
      height: 48,
      borderRadius: 4,
    },
    songInfo: {
      flex: 1,
      marginLeft: 12,
    },
    songTitle: {
      color: theme.text,
      fontFamily: Fonts.bold,
      fontSize: 16,
    },
    artistName: {
      color: '#8E8E93',
      fontFamily: Fonts.regular,
      fontSize: 14,
    },
    controls: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 16,
    },
  });

  return (
    <View style={styles.container}>
      <Image
        style={styles.albumArt}
        source={{ uri: currentTrack.thumbnail }}
      />
      <View style={styles.songInfo}>
        <Text style={styles.songTitle}>{currentTrack.title}</Text>
        <Text style={styles.artistName}>{currentTrack.artist}</Text>
      </View>
      <View style={styles.controls}>
        <Ionicons name="cast" size={24} color={theme.text} />
        <Ionicons name="play-skip-back" size={24} color={theme.text} />
        <TouchableOpacity onPress={handlePlayPause}>
          <Ionicons name={isPlaying ? 'pause' : 'play'} size={24} color={theme.text} />
        </TouchableOpacity>
        <Ionicons name="play-skip-forward" size={24} color={theme.text} />
      </View>
    </View>
  );
};

export default MiniPlayer;
