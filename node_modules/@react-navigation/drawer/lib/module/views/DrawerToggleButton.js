"use strict";

import { HeaderButton } from '@react-navigation/elements';
import { DrawerActions, useNavigation } from '@react-navigation/native';
import { Image, StyleSheet } from 'react-native';
import toggleDrawerIcon from './assets/toggle-drawer-icon.png';
import { jsx as _jsx } from "react/jsx-runtime";
export function DrawerToggleButton({
  tintColor,
  accessibilityLabel = 'Show navigation menu',
  imageSource = toggleDrawerIcon,
  ...rest
}) {
  const navigation = useNavigation();
  return /*#__PURE__*/_jsx(HeaderButton, {
    ...rest,
    accessibilityLabel: accessibilityLabel,
    onPress: () => navigation.dispatch(DrawerActions.toggleDrawer()),
    children: /*#__PURE__*/_jsx(Image, {
      resizeMode: "contain",
      source: imageSource,
      fadeDuration: 0,
      tintColor: tintColor,
      style: styles.icon
    })
  });
}
const styles = StyleSheet.create({
  icon: {
    height: 24,
    width: 24,
    marginVertical: 8,
    marginHorizontal: 5
  }
});
//# sourceMappingURL=DrawerToggleButton.js.map