"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.UNMUTE_MODE = exports.PLAY_MODE = exports.PLAYER_STATES_NAMES = exports.PLAYER_STATES = exports.PLAYER_ERROR_NAMES = exports.PLAYER_ERROR = exports.PAUSE_MODE = exports.MUTE_MODE = exports.DEFAULT_BASE_URL = exports.CUSTOM_USER_AGENT = void 0;
const PLAY_MODE = exports.PLAY_MODE = true;
const PAUSE_MODE = exports.PAUSE_MODE = false;
const MUTE_MODE = exports.MUTE_MODE = true;
const UNMUTE_MODE = exports.UNMUTE_MODE = false;
const PLAYER_STATES_NAMES = exports.PLAYER_STATES_NAMES = {
  UNSTARTED: 'unstarted',
  ENDED: 'ended',
  PLAYING: 'playing',
  PAUSED: 'paused',
  BUFFERING: 'buffering',
  VIDEO_CUED: 'video cued'
};
const PLAYER_STATES = exports.PLAYER_STATES = {
  '-1': PLAYER_STATES_NAMES.UNSTARTED,
  0: PLAYER_STATES_NAMES.ENDED,
  1: PLAYER_STATES_NAMES.PLAYING,
  2: PLAYER_STATES_NAMES.PAUSED,
  3: PLAYER_STATES_NAMES.BUFFERING,
  5: PLAYER_STATES_NAMES.VIDEO_CUED
};
const PLAYER_ERROR_NAMES = exports.PLAYER_ERROR_NAMES = {
  INVALID_PARAMETER: 'invalid_parameter',
  HTML5_ERROR: 'HTML5_error',
  VIDEO_NOT_FOUND: 'video_not_found',
  EMBED_NOT_ALLOWED: 'embed_not_allowed'
};
const PLAYER_ERROR = exports.PLAYER_ERROR = {
  2: PLAYER_ERROR_NAMES.INVALID_PARAMETER,
  5: PLAYER_ERROR_NAMES.HTML5_ERROR,
  100: PLAYER_ERROR_NAMES.VIDEO_NOT_FOUND,
  101: PLAYER_ERROR_NAMES.EMBED_NOT_ALLOWED,
  150: PLAYER_ERROR_NAMES.EMBED_NOT_ALLOWED
};
const CUSTOM_USER_AGENT = exports.CUSTOM_USER_AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36';
const DEFAULT_BASE_URL = exports.DEFAULT_BASE_URL = 'https://lonelycpp.github.io/react-native-youtube-iframe/iframe_v2.html';
//# sourceMappingURL=constants.js.map