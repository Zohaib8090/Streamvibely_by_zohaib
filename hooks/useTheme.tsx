import { createContext, useContext, useState, useMemo } from 'react';
import { ThemeProvider as RNThemeProvider, DefaultTheme } from '@react-navigation/native';
import { Colors } from '@/constants/Colors';

const ThemeContext = createContext({
  isDark: true,
  toggleTheme: () => {},
  theme: Colors.dark,
});

export const useTheme = () => useContext(ThemeContext);

export const ThemeProvider = ({ children }: { children: React.ReactNode }) => {
  const [isDark, setIsDark] = useState(true);

  const toggleTheme = () => {
    setIsDark(!isDark);
  };

  const theme = useMemo(() => (isDark ? Colors.dark : Colors.light), [isDark]);

  const navigationTheme = useMemo(
    () => ({
      ...DefaultTheme,
      colors: {
        ...DefaultTheme.colors,
        background: theme.background,
        card: theme.card,
        text: theme.text,
        primary: theme.primary,
      },
    }),
    [theme]
  );

  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme, theme }}>
      <RNThemeProvider value={navigationTheme}>{children}</RNThemeProvider>
    </ThemeContext.Provider>
  );
};
