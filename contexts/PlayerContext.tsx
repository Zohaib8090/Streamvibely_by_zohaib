
import React, { createContext, useState, useContext, useRef, useEffect } from 'react';

const PlayerContext = createContext(null);

export const PlayerProvider = ({ children }) => {
  const [currentTrack, setCurrentTrack] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isFullPlayerVisible, setFullPlayerVisible] = useState(false);
  const [isMiniPlayerVisible, setMiniPlayerVisible] = useState(false);
  const [isReady, setIsReady] = useState(false);

  const play = (track) => {
    setCurrentTrack(track);
    setIsPlaying(true);
    setMiniPlayerVisible(true);
  };

  const playVideo = () => {
    if (isReady) {
      setIsPlaying(true);
    }
  };

  const pauseTrack = () => {
    setIsPlaying(false);
  };

  const resumeTrack = () => {
    setIsPlaying(true);
  };

  const openFullPlayer = () => {
    setFullPlayerVisible(true);
    setMiniPlayerVisible(false);
  };

  const closeFullPlayer = () => {
    setFullPlayerVisible(false);
    if (currentTrack) {
      setMiniPlayerVisible(true);
    }
  };

  const value = {
    currentTrack,
    isPlaying,
    play,
    pauseTrack,
    resumeTrack,
    isFullPlayerVisible,
    openFullPlayer,
    closeFullPlayer,
    isMiniPlayerVisible,
    isReady,
    setIsReady,
    playVideo,
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  return useContext(PlayerContext);
};
