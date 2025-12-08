
import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Image, TouchableOpacity, AppState } from 'react-native';
import Slider from '@react-native-community/slider';
import { Ionicons } from '@expo/vector-icons';
import { usePlayer } from '../contexts/PlayerContext';
import { formatTime } from '../utils/formatTime';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';

const FullPlayer = () => {
  const {
    currentTrack,
    isPlaying,
    pauseTrack,
    resumeTrack,
    skipToNext,
    skipToPrevious,
    closeFullPlayer,
    toggleShuffle,
    isShuffleEnabled,
    toggleLoop,
    isLoopEnabled,
    playbackStatus,
    videoRef,
  } = usePlayer();
  const [sliderValue, setSliderValue] = useState(0);
  const [isSeeking, setIsSeeking] = useState(false);

  useEffect(() => {
    if (playbackStatus && !isSeeking) {
      const { currentTime, duration } = playbackStatus;
      const value = duration > 0 ? currentTime / duration : 0;
      setSliderValue(value);
    }
  }, [playbackStatus, isSeeking]);

  const handleSliderComplete = async (value) => {
    if (videoRef.current && playbackStatus?.duration) {
      const newPosition = value * playbackStatus.duration;
      await videoRef.current.seek(newPosition);
      setIsSeeking(false);
    }
  };

  if (!currentTrack) {
    return null;
  }
  
  const currentTime = playbackStatus?.currentTime ?? 0;
  const duration = playbackStatus?.duration ?? 0;

  return (
    <View style={styles.container}>
      <Image source={{ uri: currentTrack.image }} style={styles.albumArt} />
      <View style={styles.controlsContainer}>
        <View style={styles.trackInfo}>
          <Text style={styles.title} numberOfLines={1}>{currentTrack.title}</Text>
          <Text style={styles.artist} numberOfLines={1}>{currentTrack.artist}</Text>
        </View>
        <Slider
          style={styles.slider}
          value={sliderValue}
          minimumValue={0}
          maximumValue={1}
          minimumTrackTintColor="#FFFFFF"
          maximumTrackTintColor="#8E8E93"
          thumbTintColor="#FFFFFF"
          onSlidingStart={() => setIsSeeking(true)}
          onValueChange={(value) => setSliderValue(value)} // Update slider UI instantly
          onSlidingComplete={handleSliderComplete}
        />
        <View style={styles.timeContainer}>
          <Text style={styles.timeText}>{formatTime(currentTime * 1000)}</Text>
          <Text style={styles.timeText}>{formatTime(duration * 1000)}</Text>
        </View>
        <View style={styles.playerControls}>
          <TouchableOpacity onPress={toggleShuffle}>
            <Ionicons name={isShuffleEnabled ? "shuffle" : "shuffle-outline"} size={30} color={isShuffleEnabled ? Colors.dark.tint : Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={skipToPrevious}>
            <Ionicons name="play-skip-back" size={30} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={isPlaying ? pauseTrack : resumeTrack}>
            <Ionicons name={isPlaying ? 'pause-circle' : 'play-circle'} size={70} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={skipToNext}>
            <Ionicons name="play-skip-forward" size={30} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity onPress={toggleLoop}>
            <Ionicons name={isLoopEnabled ? "repeat" : "repeat-outline"} size={30} color={isLoopEnabled ? Colors.dark.tint : Colors.dark.text} />
          </TouchableOpacity>
        </View>
        <TouchableOpacity style={styles.backButton} onPress={closeFullPlayer}>
          <Ionicons name="chevron-down" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  albumArt: {
    width: '100%',
    height: '60%',
    resizeMode: 'cover',
  },
  controlsContainer: {
    flex: 1,
    width: '100%',
    padding: 20,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    backgroundColor: Colors.dark.background,
    marginTop: -20,
  },
  trackInfo: {
    alignItems: 'center',
    marginBottom: 20,
  },
  title: {
    color: Colors.dark.text,
    fontFamily: Fonts.bold,
    fontSize: 22,
    textAlign: 'center',
  },
  artist: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 16,
    marginTop: 5,
  },
  slider: {
    width: '100%',
    height: 40,
  },
  timeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    paddingHorizontal: 10,
  },
  timeText: {
    color: '#8E8E93',
    fontFamily: Fonts.regular,
    fontSize: 12,
  },
  playerControls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    width: '100%',
    marginTop: 20,
  },
  backButton: {
    position: 'absolute',
    top: 20,
    left: 20,
  },
});

export default FullPlayer;
