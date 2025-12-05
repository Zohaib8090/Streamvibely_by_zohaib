"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _events = require("events");
var _WebView = require("./WebView");
var _constants = require("./constants");
var _PlayerScripts = require("./PlayerScripts");
var _utils = require("./utils");
var _jsxRuntime = require("react/jsx-runtime");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const YoutubeIframe = (props, ref) => {
  const {
    height,
    width,
    videoId,
    playList,
    play = false,
    mute = false,
    volume = 100,
    viewContainerStyle,
    webViewStyle,
    webViewProps,
    useLocalHTML,
    baseUrlOverride,
    playbackRate = 1,
    contentScale = 1.0,
    onError = _err => {},
    onReady = _event => {},
    playListStartIndex = 0,
    initialPlayerParams,
    allowWebViewZoom = false,
    forceAndroidAutoplay = false,
    onChangeState = _event => {},
    onFullScreenChange = _status => {},
    onPlaybackQualityChange = _quality => {},
    onPlaybackRateChange = _playbackRate => {}
  } = props;
  const [playerReady, setPlayerReady] = (0, _react.useState)(false);
  const lastVideoIdRef = (0, _react.useRef)(videoId);
  const lastPlayListRef = (0, _react.useRef)(playList);
  const initialPlayerParamsRef = (0, _react.useRef)(initialPlayerParams || {});
  const webViewRef = (0, _react.useRef)(null);
  const eventEmitter = (0, _react.useRef)(new _events.EventEmitter());
  const sendPostMessage = (0, _react.useCallback)((eventName, meta) => {
    if (!playerReady) {
      return;
    }
    const message = JSON.stringify({
      eventName,
      meta
    });
    webViewRef.current.postMessage(message);
  }, [playerReady]);
  (0, _react.useImperativeHandle)(ref, () => ({
    getVideoUrl: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.getVideoUrlScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getVideoUrl', resolve);
      });
    },
    getDuration: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.durationScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getDuration', resolve);
      });
    },
    getCurrentTime: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.currentTimeScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getCurrentTime', resolve);
      });
    },
    isMuted: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.isMutedScript);
      return new Promise(resolve => {
        eventEmitter.current.once('isMuted', resolve);
      });
    },
    getVolume: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.getVolumeScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getVolume', resolve);
      });
    },
    getPlaybackRate: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.getPlaybackRateScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getPlaybackRate', resolve);
      });
    },
    getAvailablePlaybackRates: () => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.getAvailablePlaybackRatesScript);
      return new Promise(resolve => {
        eventEmitter.current.once('getAvailablePlaybackRates', resolve);
      });
    },
    seekTo: (seconds, allowSeekAhead) => {
      webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.seekToScript(seconds, allowSeekAhead));
    }
  }), []);
  (0, _react.useEffect)(() => {
    if (play) {
      sendPostMessage('playVideo', {});
    } else {
      sendPostMessage('pauseVideo', {});
    }
  }, [play, sendPostMessage]);
  (0, _react.useEffect)(() => {
    if (mute) {
      sendPostMessage('muteVideo', {});
    } else {
      sendPostMessage('unMuteVideo', {});
    }
  }, [mute, sendPostMessage]);
  (0, _react.useEffect)(() => {
    sendPostMessage('setVolume', {
      volume
    });
  }, [sendPostMessage, volume]);
  (0, _react.useEffect)(() => {
    sendPostMessage('setPlaybackRate', {
      playbackRate
    });
  }, [sendPostMessage, playbackRate]);
  (0, _react.useEffect)(() => {
    if (!playerReady || lastVideoIdRef.current === videoId) {
      // no instance of player is ready
      // or videoId has not changed
      return;
    }
    lastVideoIdRef.current = videoId;
    webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.loadVideoById(videoId, play));
  }, [videoId, play, playerReady]);
  (0, _react.useEffect)(() => {
    if (!playerReady) {
      // no instance of player is ready
      return;
    }

    // Also, right now, we are helping users by doing "deep" comparisons of playList prop,
    // but in the next major we should leave the responsibility to user (either via useMemo or moving the array outside)
    if (!playList || (0, _utils.deepComparePlayList)(lastPlayListRef.current, playList)) {
      return;
    }
    lastPlayListRef.current = playList;
    webViewRef.current.injectJavaScript(_PlayerScripts.PLAYER_FUNCTIONS.loadPlaylist(playList, playListStartIndex, play));
  }, [playList, play, playListStartIndex, playerReady]);
  const onWebMessage = (0, _react.useCallback)(event => {
    try {
      const message = JSON.parse(event.nativeEvent.data);
      switch (message.eventType) {
        case 'fullScreenChange':
          onFullScreenChange(message.data);
          break;
        case 'playerStateChange':
          onChangeState(_constants.PLAYER_STATES[message.data]);
          break;
        case 'playerReady':
          onReady();
          setPlayerReady(true);
          break;
        case 'playerQualityChange':
          onPlaybackQualityChange(message.data);
          break;
        case 'playerError':
          onError(_constants.PLAYER_ERROR[message.data]);
          break;
        case 'playbackRateChange':
          onPlaybackRateChange(message.data);
          break;
        default:
          eventEmitter.current.emit(message.eventType, message.data);
          break;
      }
    } catch (error) {
      console.warn('[rn-youtube-iframe]', error);
    }
  }, [onReady, onError, onChangeState, onFullScreenChange, onPlaybackRateChange, onPlaybackQualityChange]);
  const onShouldStartLoadWithRequest = (0, _react.useCallback)(request => {
    try {
      const url = request.mainDocumentURL || request.url;
      if (_reactNative.Platform.OS === 'ios') {
        const iosFirstLoad = url === 'about:blank';
        if (iosFirstLoad) {
          return true;
        }
        const isYouTubeLink = url.startsWith('https://www.youtube.com/');
        if (isYouTubeLink) {
          _reactNative.Linking.openURL(url).catch(error => {
            console.warn('Error opening URL:', error);
          });
          return false;
        }
      }
      return url.startsWith(baseUrlOverride || _constants.DEFAULT_BASE_URL);
    } catch (error) {
      // defaults to true in case of error
      // returning false stops the video from loading
      return true;
    }
  }, [baseUrlOverride]);
  const source = (0, _react.useMemo)(() => {
    const ytScript = (0, _PlayerScripts.MAIN_SCRIPT)(lastVideoIdRef.current, lastPlayListRef.current, initialPlayerParamsRef.current, allowWebViewZoom, contentScale);
    if (useLocalHTML) {
      const res = {
        html: ytScript.htmlString
      };
      if (baseUrlOverride) {
        res.baseUrl = baseUrlOverride;
      }
      return res;
    }
    const base = baseUrlOverride || _constants.DEFAULT_BASE_URL;
    const data = ytScript.urlEncodedJSON;
    return {
      uri: base + '?data=' + data
    };
  }, [useLocalHTML, contentScale, baseUrlOverride, allowWebViewZoom]);
  return /*#__PURE__*/(0, _jsxRuntime.jsx)(_reactNative.View, {
    style: [{
      height,
      width
    }, viewContainerStyle],
    children: /*#__PURE__*/(0, _jsxRuntime.jsx)(_WebView.WebView, {
      bounces: false,
      originWhitelist: ['*'],
      allowsInlineMediaPlayback: true,
      style: [styles.webView, webViewStyle],
      mediaPlaybackRequiresUserAction: false,
      onShouldStartLoadWithRequest: onShouldStartLoadWithRequest,
      allowsFullscreenVideo: !initialPlayerParamsRef.current.preventFullScreen,
      userAgent: forceAndroidAutoplay ? _reactNative.Platform.select({
        android: _constants.CUSTOM_USER_AGENT,
        ios: ''
      }) : ''
      // props above this are override-able

      // --
      ,
      ...webViewProps,
      // --

      // add props that should not be allowed to be overridden below
      source: source,
      ref: webViewRef,
      onMessage: onWebMessage
    })
  });
};
const styles = _reactNative.StyleSheet.create({
  webView: {
    backgroundColor: 'transparent'
  }
});
var _default = exports.default = /*#__PURE__*/(0, _react.forwardRef)(YoutubeIframe);
//# sourceMappingURL=YoutubeIframe.js.map