
import React from 'react';
import Modal from 'react-native-modal';
import YoutubeIframe from 'react-native-youtube-iframe';
import { usePlayer } from '../contexts/PlayerContext';

const VideoPlayer = () => {
  const { currentTrack, isPlaying, pauseTrack } = usePlayer();

  if (!currentTrack) {
    return null;
  }

  return (
    <Modal
      isVisible={isPlaying}
      onBackdropPress={pauseTrack}
      onBackButtonPress={pauseTrack}
      style={{ margin: 0, justifyContent: 'center', alignItems: 'center' }}
    >
      <YoutubeIframe
        height={300}
        width={400}
        videoId={currentTrack.id}
        play={isPlaying}
        onChangeState={(state) => {
          if (state === 'paused') {
            pauseTrack();
          }
        }}
      />
    </Modal>
  );
};

export default VideoPlayer;
