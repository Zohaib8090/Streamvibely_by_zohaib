
import React, { createContext, useState, useContext, useEffect, useRef } from 'react';
import { Video } from 'expo-video';
import WebView from 'react-native-webview';

const PlayerContext = createContext(null);

export const PlayerProvider = ({ children }) => {
  const [currentTrack, setCurrentTrack] = useState(null);
  const [playlist, setPlaylist] = useState([]);
  const [originalPlaylist, setOriginalPlaylist] = useState([]);
  const [history, setHistory] = useState([]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playerState, setPlayerState] = useState('hidden'); // hidden, mini, full
  const [isReady, setIsReady] = useState(false);
  const [isShuffleEnabled, setShuffleEnabled] = useState(false);
  const [isLoopEnabled, setLoopEnabled] = useState(false);
  const [playbackStatus, setPlaybackStatus] = useState(null);
  const [isNowVideo, setIsNowVideo] = useState(false);

  const videoRef = useRef(null);
  const [fetchingTrack, setFetchingTrack] = useState(null); // { id: string, isVideo: boolean }

  const handleEnd = () => {
    if (isLoopEnabled) {
      videoRef.current?.replay();
    } else {
      skipToNext();
    }
  };

  const handleProgress = (status) => {
    const newStatus = {
        positionMillis: status.position * 1000,
        durationMillis: status.duration * 1000,
        isPlaying: isPlaying,
        isLoaded: true,
    };
    setPlaybackStatus(newStatus);
  };

  const handleStatusChange = (status) => {
    setIsPlaying(status === 'playing');
  }

  const handleStreamUrlFetched = (streamUrl, trackId, isVideo) => {
    const trackToPlay = playlist.find(t => t.id === trackId) || history.find(t => t.id === trackId) || { id: trackId };

    if (!trackToPlay) {
      console.error("Could not find track after fetching stream.");
      setFetchingTrack(null);
      return;
    }

    const trackWithUri = { ...trackToPlay, uri: streamUrl };
    
    videoRef.current?.load(trackWithUri.uri);
    videoRef.current?.play();

    setCurrentTrack(trackWithUri);
    setIsPlaying(true);
    setIsNowVideo(isVideo);

    if (!isVideo) {
        setPlayerState(playerState === 'hidden' ? 'mini' : playerState);
    }

    setHistory(prevHistory => {
      const newHistory = [trackWithUri, ...prevHistory.filter(t => t.id !== trackWithUri.id)];
      return newHistory.slice(0, 50);
    });
    setFetchingTrack(null);
  };

  const play = (track, newPlaylist, options = {}) => {
    if (!track || !track.id) {
      console.error('Cannot play track: Invalid track object');
      return;
    }
    
    const { openFullPlayer, isVideo = false } = options;

    if (openFullPlayer && !isVideo) {
      setPlayerState('full');
    }

    if (newPlaylist) {
      setPlaylist(newPlaylist);
      setOriginalPlaylist(newPlaylist);
    }
    
    setFetchingTrack({ id: track.id, isVideo });
  };

  const pauseTrack = () => {
    videoRef.current?.pause();
    setIsPlaying(false);
  };

  const resumeTrack = () => {
    videoRef.current?.play();
    setIsPlaying(true);
  };

  const closeVideoPlayer = () => {
    videoRef.current?.stop();
    setIsNowVideo(false);
    setIsPlaying(false);
    setCurrentTrack(null);
  };

  const skipToNext = () => {
    if (!currentTrack) return;
    const isVideo = isNowVideo;
    const currentIndex = playlist.findIndex(t => t.id === currentTrack.id);

    if (currentIndex === playlist.length - 1) {
      if (isLoopEnabled) {
        play(playlist[0], playlist, { isVideo });
      } else {
        setIsPlaying(false);
        if (!isVideo) setPlayerState('hidden');
      }
    } else {
      const nextTrack = playlist[currentIndex + 1];
      play(nextTrack, playlist, { isVideo });
    }
  };

  const skipToPrevious = () => {
    if (!currentTrack) return;
    const isVideo = isNowVideo;
    const currentIndex = playlist.findIndex(t => t.id === currentTrack.id);

    if (currentIndex > 0) {
      const prevTrack = playlist[currentIndex - 1];
      play(prevTrack, playlist, { isVideo });
    }
  };

  const toggleShuffle = () => {
    const newShuffleState = !isShuffleEnabled;
    setShuffleEnabled(newShuffleState);

    if (newShuffleState) {
      const otherTracks = originalPlaylist.filter(t => t.id !== currentTrack.id);
      for (let i = otherTracks.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [otherTracks[i], otherTracks[j]] = [otherTracks[j], otherTracks[i]];
      }
      if (currentTrack) {
        setPlaylist([currentTrack, ...otherTracks]);
      } else {
        setPlaylist(otherTracks);
      }
    } else {
      setPlaylist(originalPlaylist);
    }
  };

  const toggleLoop = () => {
    setLoopEnabled(prev => !prev);
  };

  const openFullPlayer = () => {
    if (!isNowVideo) setPlayerState('full');
  };

  const closeFullPlayer = () => {
    setPlayerState(currentTrack ? 'mini' : 'hidden');
  };

  const StreamFetcher = () => {
    if (!fetchingTrack) return null;

    const { id, isVideo } = fetchingTrack;

    const injectedJavaScript = `
      (function() {
        fetch('https://pipedapi.kavin.rocks/streams/' + '${id}')
          .then(response => response.json())
          .then(data => {
            let stream;
            if (${isVideo}) {
              stream = data.videoStreams.find(s => s.quality === '1080p') || data.videoStreams.find(s => s.quality === '720p');
            } else {
              stream = data.audioStreams.find(s => s.mimeType === 'audio/mp4') || data.audioStreams[0];
            }

            if (stream && stream.url) {
              window.ReactNativeWebView.postMessage(JSON.stringify({
                type: 'SUCCESS',
                payload: { url: stream.url, trackId: '${id}', isVideo: ${isVideo} }
              }));
            } else {
              throw new Error('No suitable stream found.');
            }
          })
          .catch(error => {
            window.ReactNativeWebView.postMessage(JSON.stringify({
              type: 'ERROR',
              payload: { message: error.message, trackId: '${id}' }
            }));
          });
      })();
      true;
    `;

    return (
      <WebView
        key={id}
        style={{ width: 1, height: 1, position: 'absolute', top: -1000, left: -1000 }}
        source={{ html: '<html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head><body></body></html>' }}
        injectedJavaScript={injectedJavaScript}
        onMessage={(event) => {
          try {
            const data = JSON.parse(event.nativeEvent.data);
            if (data.type === 'SUCCESS') {
              handleStreamUrlFetched(data.payload.url, data.payload.trackId, data.payload.isVideo);
            } else {
              console.error('Serverless fetch failed:', data.payload.message);
              setFetchingTrack(null);
            }
          } catch (e) {
            console.error("Failed to parse message from WebView", e);
          }
        }}
        onError={(syntheticEvent) => {
          const { nativeEvent } = syntheticEvent;
          console.error('WebView error: ', nativeEvent);
          setFetchingTrack(null);
        }}
      />
    );
  };

  const value = {
    currentTrack,
    playlist,
    history,
    isPlaying,
    play,
    pauseTrack,
    resumeTrack,
    skipToNext,
    skipToPrevious,
    playerState,
    openFullPlayer,
    closeFullPlayer,
    isReady,
    setIsReady,
    isShuffleEnabled,
    toggleShuffle,
    isLoopEnabled,
    toggleLoop,
    playbackStatus,
    videoRef,
    isNowVideo,
    closeVideoPlayer,
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <StreamFetcher />
      <Video
        ref={videoRef}
        style={{ width: 0, height: 0 }} // Keep it hidden
        onEnd={handleEnd}
        onProgress={handleProgress}
        onStatusChange={handleStatusChange}
      />
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  const context = useContext(PlayerContext);
  if (context === undefined) {
    throw new Error('usePlayer must be used within a PlayerProvider');
  }
  return context;
};
