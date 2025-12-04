
import React, { createContext, useState, useContext, useRef, useEffect } from 'react';
import { WebView } from 'react-native-webview';

const PlayerContext = createContext(null);

export const PlayerProvider = ({ children }) => {
  const [currentTrack, setCurrentTrack] = useState(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isPlayerReady, setIsPlayerReady] = useState(false);
  const webViewRef = useRef(null);

  const playTrack = (track) => {
    setCurrentTrack(track);
  };

  const pauseTrack = () => {
    if (webViewRef.current) {
      webViewRef.current.injectJavaScript('player.pauseVideo();');
      setIsPlaying(false);
    }
  };

  const resumeTrack = () => {
    if (webViewRef.current) {
      webViewRef.current.injectJavaScript('player.playVideo();');
      setIsPlaying(true);
    }
  };

  useEffect(() => {
    if (currentTrack && isPlayerReady && webViewRef.current) {
      webViewRef.current.injectJavaScript(`
        player.loadVideoById('${currentTrack.id}');
      `);
    }
  }, [currentTrack, isPlayerReady]);

  const value = {
    currentTrack,
    isPlaying,
    playTrack,
    pauseTrack,
    resumeTrack,
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <WebView
        ref={webViewRef}
        style={{ position: 'absolute', top: -9999, left: -9999 }}
        javaScriptEnabled={true}
        domStorageEnabled={true}
        mediaPlaybackRequiresUserAction={false}
        onMessage={(event) => {
          const message = JSON.parse(event.nativeEvent.data);
          if (message.event === 'onReady') {
            setIsPlayerReady(true);
          } else if (message.event === 'onStateChange') {
            if (message.data === 1) { // Playing
              setIsPlaying(true);
            } else { // Paused, ended, etc.
              setIsPlaying(false);
            }
          }
        }}
        source={{
          html: `
            <div id="player"></div>
            <script>
              var tag = document.createElement('script');
              tag.src = "https://www.youtube.com/iframe_api";
              var firstScriptTag = document.getElementsByTagName('script')[0];
              firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '0',
                  width: '0',
                  events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange
                  }
                });
              }

              function onPlayerReady(event) {
                window.ReactNativeWebView.postMessage(JSON.stringify({ event: 'onReady' }));
              }

              function onPlayerStateChange(event) {
                window.ReactNativeWebView.postMessage(JSON.stringify({ event: 'onStateChange', data: event.data }));
              }
            </script>
          `,
        }}
      />
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  return useContext(PlayerContext);
};
