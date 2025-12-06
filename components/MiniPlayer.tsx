
import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { usePlayer } from '../contexts/PlayerContext';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

const MiniPlayer = () => {
  const { isMiniPlayerVisible, currentTrack, isPlaying, pauseTrack, resumeTrack, openFullPlayer } = usePlayer();

  const handlePlayPause = (e) => {
    e.stopPropagation(); // Prevent the full player from opening
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
    <TouchableOpacity onPress={openFullPlayer} style={styles.container}>
      <Image source={{ uri: currentTrack.thumbnail }} style={styles.thumbnail} />
      <View style={styles.trackInfo}>
        <Text style={styles.title} numberOfLines={1}>{currentTrack.title}</Text>
        <Text style={styles.artist} numberOfLines={1}>{currentTrack.artist}</Text>
      </View>
      <View style={styles.controls}>
        <TouchableOpacity>
            <MaterialCommunityIcons name="cast" size={24} color="#fff" />
        </TouchableOpacity>
        <TouchableOpacity onPress={handlePlayPause}>
          <Ionicons name={isPlaying ? 'pause' : 'play'} size={30} color="#fff" />
        </TouchableOpacity>
        <TouchableOpacity>
          <Ionicons name="play-skip-forward" size={30} color="#fff" />
        </TouchableOpacity>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 0, // Adjust this value if you have a tab bar
    left: 0,
    right: 0,
    height: 70,
    backgroundColor: '#2a2a2a', // Dark background color
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    borderTopWidth: 1,
    borderTopColor: '#444',
  },
  thumbnail: {
    width: 50,
    height: 50,
    borderRadius: 5,
  },
  trackInfo: {
    flex: 1,
    marginLeft: 10,
    justifyContent: 'center',
  },
  title: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#fff',
  },
  artist: {
    fontSize: 14,
    color: '#aaa',
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
});

export default MiniPlayer;
