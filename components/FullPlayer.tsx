
import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Dimensions, Platform } from 'react-native';
import Modal from 'react-native-modal';
import { usePlayer } from '../contexts/PlayerContext';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import Slider from '@react-native-community/slider';

// Platform-specific imports
import YoutubeIframe from 'react-native-youtube-iframe';
import YouTube from 'react-youtube';

const { width } = Dimensions.get('window');

const FullPlayer = () => {
  const {
    isFullPlayerVisible,
    closeFullPlayer,
    currentTrack,
    isPlaying,
    pauseTrack,
    resumeTrack,
    setIsReady,
  } = usePlayer();
  const [playerMode, setPlayerMode] = useState('song'); // 'song' or 'video'
  const [thumbnailUrl, setThumbnailUrl] = useState('');

  const webPlayerRef = useRef<any>(null);

  // Set the highest quality thumbnail available when the track changes
  useEffect(() => {
    if (currentTrack) {
      // Start by trying to load the max resolution image
      setThumbnailUrl(`https://img.youtube.com/vi/${currentTrack.id}/maxresdefault.jpg`);
    }
  }, [currentTrack]);

  const handleThumbnailError = () => {
    // If maxresdefault fails, fall back to hqdefault
    setThumbnailUrl(`https://img.youtube.com/vi/${currentTrack.id}/hqdefault.jpg`);
  };

  useEffect(() => {
    if (Platform.OS === 'web' && webPlayerRef.current) {
      if (isPlaying) {
        webPlayerRef.current.playVideo();
      } else {
        webPlayerRef.current.pauseVideo();
      }
    }
  }, [isPlaying, webPlayerRef.current]);

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

  const handleWebPlayerReady = (event: any) => {
    webPlayerRef.current = event.target;
    setIsReady(true);
  };

  const handleWebPlayerStateChange = (event: any) => {
    if (event.data === 1 && !isPlaying) {
      resumeTrack();
    } else if (event.data === 2 && isPlaying) {
      pauseTrack();
    }
  };

  const handleNativePlayerStateChange = (state: string) => {
    if (state === 'paused' && isPlaying) {
      pauseTrack();
    } else if (state === 'playing' && !isPlaying) {
      resumeTrack();
    } else if (state === 'ended') {
      pauseTrack();
    }
  };

  const renderPlayer = () => {
    const videoContainerStyle = playerMode === 'video'
      ? styles.videoPlayerVisible
      : styles.videoPlayerHidden;

    if (Platform.OS === 'web') {
      const opts = {
        height: '100%',
        width: '100%',
        playerVars: {
          autoplay: 1,
          controls: 0,
        },
      };

      return (
        <View style={videoContainerStyle}>
          <YouTube
            videoId={currentTrack.id}
            opts={opts}
            onReady={handleWebPlayerReady}
            onStateChange={handleWebPlayerStateChange}
            containerClassName='youtube-container'
          />
        </View>
      );
    }

    // For Android/iOS
    return (
      <View style={videoContainerStyle}>
        <YoutubeIframe
          height={width} 
          videoId={currentTrack.id}
          play={isPlaying}
          onReady={() => setIsReady(true)}
          onChangeState={handleNativePlayerStateChange}
        />
      </View>
    );
  };

  return (
    <Modal
      isVisible={isFullPlayerVisible}
      style={styles.modal}
      onBackButtonPress={closeFullPlayer}
      onBackdropPress={closeFullPlayer}
      animationIn="slideInUp"
      animationOut="slideOutDown"
      backdropOpacity={1}
      backdropColor="#000"
      useNativeDriverForBackdrop
    >
      <View style={styles.container}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={closeFullPlayer}>
            <Ionicons name="chevron-down" size={28} color="#fff" />
          </TouchableOpacity>
          <View style={styles.toggleContainer}>
            <TouchableOpacity
              onPress={() => setPlayerMode('song')}
              style={[
                styles.toggleButton,
                playerMode === 'song' && styles.activeButton,
              ]}
            >
              <Text style={styles.toggleText}>Song</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => setPlayerMode('video')}
              style={[
                styles.toggleButton,
                playerMode === 'video' && styles.activeButton,
              ]}
            >
              <Text style={styles.toggleText}>Video</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.headerRight}>
            <TouchableOpacity>
              <MaterialCommunityIcons name="cast" size={24} color="#fff" />
            </TouchableOpacity>
            <TouchableOpacity>
              <Ionicons name="ellipsis-vertical" size={24} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>

        {/* Content */}
        <View style={styles.content}>
           {thumbnailUrl ? (
             <Image
                source={{ uri: thumbnailUrl }}
                style={styles.albumArt}
                resizeMode="cover"
                onError={handleThumbnailError} // This is the fallback mechanism
              />
            ) : null}
          {renderPlayer()}
        </View>

        {/* Details */}
        <View style={styles.detailsContainer}>
          <Text style={styles.title} numberOfLines={1}>{currentTrack.title}</Text>
          <Text style={styles.artist}>{currentTrack.artist}</Text>
        </View>

        {/* Actions */}
        <View style={styles.actionsContainer}>
          <TouchableOpacity style={styles.actionButton}>
            <Ionicons name="thumbs-up-outline" size={22} color="#fff" />
            <Text style={styles.actionText}>1.2M</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionButton}>
            <Ionicons name="thumbs-down-outline" size={22} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionButton}>
            <Ionicons
              name="chatbubble-ellipses-outline"
              size={22}
              color="#fff"
            />
            <Text style={styles.actionText}>32K</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionButton}>
            <MaterialCommunityIcons
              name="playlist-plus"
              size={24}
              color="#fff"
            />
            <Text style={styles.actionText}>Save</Text>
          </TouchableOpacity>
        </View>

        {/* Progress Bar */}
        <View style={styles.progressContainer}>
          <Slider
            style={styles.slider}
            minimumValue={0}
            maximumValue={1}
            minimumTrackTintColor="#FFFFFF"
            maximumTrackTintColor="#AAAAAA"
            thumbTintColor="#FFFFFF"
          />
          <View style={styles.timeContainer}>
            <Text style={styles.timeText}>0:01</Text>
            <Text style={styles.timeText}>1:36</Text>
          </View>
        </View>

        {/* Controls */}
        <View style={styles.controlsContainer}>
          <TouchableOpacity>
            <Ionicons name="shuffle" size={30} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="play-skip-back" size={30} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.playButton}
            onPress={handlePlayPause}
          >
            <Ionicons
              name={isPlaying ? 'pause' : 'play'}
              size={50}
              color="#000"
            />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="play-skip-forward" size={30} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="repeat" size={30} color="#fff" />
          </TouchableOpacity>
        </View>

        {/* Tabs */}
        <View style={styles.tabsContainer}>
          <TouchableOpacity style={styles.tabButton}>
            <Text style={styles.tabText}>UP NEXT</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tabButton, styles.activeTab]}
          >
            <Text style={styles.tabText}>LYRICS</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.tabButton}>
            <Text style={styles.tabText}>RELATED</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modal: {
    margin: 0,
  },
  container: {
    flex: 1,
    backgroundColor: '#000',
    padding: 15,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
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
    fontWeight: 'bold',
  },
  content: {
    marginVertical: 20,
    alignItems: 'center',
    height: width - 40,
    justifyContent: 'center',
    backgroundColor: '#000',
    overflow: 'hidden', // Prevent off-screen player from creating scroll
  },
  albumArt: {
    width: '100%',
    height: '100%',
    borderRadius: 10,
    position: 'absolute',
  },
  videoPlayerVisible: {
    width: '100%',
    aspectRatio: 16 / 9,
    backgroundColor: 'black',
  },
  videoPlayerHidden: {
    position: 'absolute',
    top: -9999, // Move far off-screen
    left: -9999,
    width: 1,
    height: 1,
  },
  detailsContainer: {
    alignItems: 'flex-start',
    width: '100%',
    marginTop: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#fff',
  },
  artist: {
    fontSize: 18,
    color: '#aaa',
    marginTop: 5,
  },
  actionsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 20,
    paddingHorizontal: 10,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  actionText: {
    color: '#fff',
    fontSize: 14,
  },
  progressContainer: {
    width: '100%',
    marginVertical: 10,
  },
  slider: {
    width: '100%',
    height: 40,
  },
  timeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 10,
  },
  timeText: {
    color: '#aaa',
    fontSize: 12,
  },
  controlsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    marginVertical: 20,
  },
  playButton: {
    backgroundColor: 'white',
    borderRadius: 50,
    width: 80,
    height: 80,
    justifyContent: 'center',
    alignItems: 'center',
  },
  tabsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    borderTopWidth: 1,
    borderTopColor: '#333',
    paddingVertical: 15,
  },
  tabButton: {
    paddingHorizontal: 10,
  },
  activeTab: {
    borderBottomWidth: 2,
    borderBottomColor: '#fff',
  },
  tabText: {
    color: '#aaa',
    fontSize: 14,
    fontWeight: 'bold',
  },
});

export default FullPlayer;
