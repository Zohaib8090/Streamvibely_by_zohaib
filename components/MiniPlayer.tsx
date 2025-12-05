
import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { usePlayer } from '../contexts/PlayerContext';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '@/hooks/useTheme';

const MiniPlayer = () => {
  const { isMiniPlayerVisible, currentTrack, isPlaying, pauseTrack, resumeTrack, openFullPlayer } = usePlayer();
  const theme = useTheme();

  const handlePlayPause = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      resumeTrack();
    }
  };
  
  if (!isMiniPlayerVisible || !currentTrack) {
    return null;
  }

  return (
    <TouchableOpacity onPress={openFullPlayer} style={[styles.container, { backgroundColor: theme.card }]}>
        <Image source={{ uri: currentTrack.thumbnail }} style={styles.thumbnail} />
        <View style={styles.trackInfo}>
            <Text style={[styles.title, { color: theme.text }]}>{currentTrack.title}</Text>
            <Text style={[styles.artist, { color: theme.textSecondary }]}>{currentTrack.artist}</Text>
        </View>
        <TouchableOpacity onPress={handlePlayPause} style={styles.playButton}>
            <Ionicons name={isPlaying ? 'pause' : 'play'} size={24} color={theme.text} />
        </TouchableOpacity>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        height: 60,
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 10,
      },
      thumbnail: {
        width: 40,
        height: 40,
        borderRadius: 5,
      },
      trackInfo: {
        flex: 1,
        marginLeft: 10,
      },
      title: {
        fontSize: 16,
        fontWeight: 'bold',
      },
      artist: {
        fontSize: 14,
      },
      playButton: {
        padding: 10,
      },
});

export default MiniPlayer;
