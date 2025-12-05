
import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import Modal from 'react-native-modal';
import YoutubeIframe from 'react-native-youtube-iframe';
import { usePlayer } from '../contexts/PlayerContext';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useTheme } from '@/hooks/useTheme';

const FullPlayer = () => {
  const { isFullPlayerVisible, closeFullPlayer, currentTrack, isPlaying, pauseTrack, resumeTrack } = usePlayer();
  const [playerMode, setPlayerMode] = useState('song'); // 'song' or 'video'
  const theme = useTheme();

  const handlePlayPause = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      resumeTrack();
    }
  };
  
  if (!currentTrack) {
    return null;
  }

  return (
    <Modal
      isVisible={isFullPlayerVisible}
      style={styles.modal}
      onBackButtonPress={closeFullPlayer}
      onBackdropPress={closeFullPlayer}
      animationIn="slideInUp"
      animationOut="slideOutDown"
      backdropOpacity={0}
      useNativeDriverForBackdrop
    >
      <View style={[styles.container, { backgroundColor: theme.background }]}>
        {/* Header */}
        <View style={styles.header}>
            <TouchableOpacity onPress={closeFullPlayer}>
                <Ionicons name="chevron-down" size={24} color={theme.text} />
            </TouchableOpacity>
            {/* Song/Video Toggle */}
            <View style={styles.toggleContainer}>
                <TouchableOpacity onPress={() => setPlayerMode('song')} style={[styles.toggleButton, playerMode === 'song' && styles.activeButton]}>
                    <Text style={styles.toggleText}>Song</Text>
                </TouchableOpacity>
                <TouchableOpacity onPress={() => setPlayerMode('video')} style={[styles.toggleButton, playerMode === 'video' && styles.activeButton]}>
                    <Text style={styles.toggleText}>Video</Text>
                </TouchableOpacity>
            </View>
            <TouchableOpacity>
                <Ionicons name="ellipsis-vertical" size={24} color={theme.text} />
            </TouchableOpacity>
        </View>

        {/* Content */}
        <View style={styles.content}>
          {playerMode === 'song' ? (
            <Image source={{ uri: currentTrack.thumbnail }} style={styles.albumArt} />
          ) : (
            <YoutubeIframe
              height={220}
              videoId={currentTrack.id}
              play={isPlaying}
              onChangeState={(state) => {
                if (state === 'paused') pauseTrack();
                if (state === 'playing') resumeTrack();
              }}
            />
          )}
        </View>

        {/* Details */}
        <View style={styles.detailsContainer}>
            <Text style={[styles.title, { color: theme.text }]}>{currentTrack.title}</Text>
            <Text style={[styles.artist, { color: theme.textSecondary }]}>{currentTrack.artist}</Text>
        </View>

        {/* Actions */}
        <View style={styles.actionsContainer}>
            {/* Placeholder for likes, etc. */}
        </View>

        {/* Progress Bar */}
        <View style={styles.progressContainer}>
            {/* Placeholder for slider */}
        </View>

        {/* Controls */}
        <View style={styles.controlsContainer}>
            <TouchableOpacity>
                <Ionicons name="shuffle" size={30} color={theme.text} />
            </TouchableOpacity>
            <TouchableOpacity>
                <Ionicons name="play-skip-back" size={30} color={theme.text} />
            </TouchableOpacity>
            <TouchableOpacity style={styles.playButton} onPress={handlePlayPause}>
                <Ionicons name={isPlaying ? 'pause' : 'play'} size={40} color={theme.background} />
            </TouchableOpacity>
            <TouchableOpacity>
                <Ionicons name="play-skip-forward" size={30} color={theme.text} />
            </TouchableOpacity>
            <TouchableOpacity>
                <Ionicons name="repeat" size={30} color={theme.text} />
            </TouchableOpacity>
        </View>

        {/* Tabs */}
        <View style={styles.tabsContainer}>
            {/* Placeholder for tabs */}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modal: {
    margin: 0,
    justifyContent: 'flex-end',
  },
  container: {
    height: '100%',
    width: '100%',
    borderTopRightRadius: 20,
    borderTopLeftRadius: 20,
    padding: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  toggleContainer: {
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: '#555',
    borderRadius: 20,
  },
  toggleButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
  },
  activeButton: {
    backgroundColor: '#555',
  },
  toggleText: {
    color: 'white',
  },
  content: {
    marginTop: 30,
    alignItems: 'center',
  },
  albumArt: {
    width: '100%',
    height: 300,
    borderRadius: 10,
  },
  detailsContainer: {
    marginTop: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  artist: {
    fontSize: 18,
    marginTop: 5,
  },
  actionsContainer: {
    marginTop: 20,
  },
  progressContainer: {
    marginTop: 20,
  },
  controlsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    marginTop: 30,
  },
  playButton: {
    backgroundColor: 'white',
    borderRadius: 50,
    padding: 20,
  },
  tabsContainer: {
    marginTop: 30,
  }
});

export default FullPlayer;
