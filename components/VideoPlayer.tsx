
import React from 'react';
import Modal from 'react-native-modal';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { Video } from 'expo-video';
import { Ionicons } from '@expo/vector-icons';
import { usePlayer } from '../contexts/PlayerContext';
import { Colors } from '@/constants/Colors';

const VideoPlayer = () => {
  const {
    isNowVideo,
    closeVideoPlayer,
    pauseTrack,
    resumeTrack,
    isPlaying,
    videoRef,
    currentTrack
  } = usePlayer();

  return (
    <Modal
      isVisible={isNowVideo}
      style={styles.modal}
      onBackdropPress={closeVideoPlayer}
      onBackButtonPress={closeVideoPlayer}
      animationIn="fadeIn"
      animationOut="fadeOut"
      backdropOpacity={0}
    >
      <View style={styles.videoContainer}>
        <Video
            ref={videoRef}
            style={StyleSheet.absoluteFill}
            source={{ uri: currentTrack?.uri }}
            resizeMode="contain"
            />
      </View>
      <TouchableOpacity style={styles.closeButton} onPress={closeVideoPlayer}>
        <Ionicons name="close" size={32} color="white" />
      </TouchableOpacity>
      <View style={styles.controls}>
        <TouchableOpacity onPress={isPlaying ? pauseTrack : resumeTrack}>
            <Ionicons name={isPlaying ? 'pause-circle' : 'play-circle'} size={70} color={Colors.dark.text} />
        </TouchableOpacity>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modal: {
    margin: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'black',
  },
  videoContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  controls: {
    position: 'absolute',
    bottom: 50,
    alignSelf: 'center',
  },
  closeButton: {
    position: 'absolute',
    top: 40,
    right: 20,
    zIndex: 10,
  },
});

export default VideoPlayer;
