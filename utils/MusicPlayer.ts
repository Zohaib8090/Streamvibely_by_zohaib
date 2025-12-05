
import AsyncStorage from '@react-native-async-storage/async-storage';

const HISTORY_KEY = 'playback_history';

export const playTrack = async (track) => {
  try {
    const history = await getHistory();
    const newHistory = [track, ...history];
    await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(newHistory));
  } catch (error) {
    console.error('Error saving history:', error);
  }
};

export const getHistory = async () => {
  try {
    const historyJson = await AsyncStorage.getItem(HISTORY_KEY);
    return historyJson ? JSON.parse(historyJson) : [];
  } catch (error) {
    console.error('Error fetching history:', error);
    return [];
  }
};

export const clearHistory = async () => {
  try {
    await AsyncStorage.removeItem(HISTORY_KEY);
  } catch (error) {
    console.error('Error clearing history:', error);
  }
};
