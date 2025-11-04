
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { useTheme } from '@react-navigation/native';
import { Link } from 'expo-router';
import React from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';

const login = () => {
  const { dark } = useTheme();
  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}>
      <SafeAreaView style={styles.container}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            paddingTop: 10,
            paddingHorizontal: 20,
          }}>
          <View style={styles.logoContainer}>
            <Image
              source={require('@/assets/images/icon.png')}
              style={{
                width: 50,
                height: 50,
                tintColor: Colors.light.tint,
              }}
            />
          </View>
          <Text style={styles.title}>Welcome back to Streamvibely</Text>
          <Text style={styles.subtitle}>Sign in to continue</Text>
          <TouchableOpacity style={styles.googleButton}>
            <Image
              source={{
                uri: 'https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg',
              }}
              style={styles.googleIcon}
            />
            <Text style={styles.googleButtonText}>Sign in with Google</Text>
          </TouchableOpacity>
          <View style={styles.dividerContainer}>
            <View style={styles.divider} />
            <Text style={styles.dividerText}>OR CONTINUE WITH</Text>
            <View style={styles.divider} />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>Email</Text>
            <TextInput
              style={styles.input}
              placeholder="m@example.com"
              placeholderTextColor={Colors.dark.text}
              keyboardType="email-address"
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputContainer}>
            <View style={styles.passwordLabelContainer}>
              <Text style={styles.inputLabel}>Password</Text>
              <Link href="/forgot-password" asChild>
                <Text style={styles.forgotPassword}>Forgot password?</Text>
              </Link>
            </View>
            <TextInput
              style={styles.input}
              placeholder="************"
              placeholderTextColor={Colors.dark.text}
              secureTextEntry
            />
          </View>

          <TouchableOpacity style={styles.signInButton}>
            <Text style={styles.signInButtonText}>Sign In</Text>
          </TouchableOpacity>
          <View style={styles.signUpContainer}>
            <Text style={styles.signUpText}>Don't have an account?</Text>
            <Link href="/signup" asChild>
              <Text style={styles.signUpLink}> Sign up</Text>
            </Link>
          </View>
        </ScrollView>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
};

export default login;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
  },
  logoContainer: {
    alignItems: 'center',
    marginVertical: 20,
  },
  title: {
    fontSize: 24,
    color: Colors.dark.text,
    textAlign: 'center',
    fontFamily: Fonts.bold,
  },
  subtitle: {
    fontSize: 16,
    color: Colors.dark.text,
    textAlign: 'center',
    marginBottom: 20,
    fontFamily: Fonts.regular,
  },
  googleButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.dark.background,
    borderRadius: 8,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: Colors.dark.text,
  },
  googleIcon: {
    width: 24,
    height: 24,
    marginRight: 10,
  },
  googleButtonText: {
    fontSize: 16,
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
  },
  dividerContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 20,
  },
  divider: {
    flex: 1,
    height: 1,
    backgroundColor: Colors.dark.text,
  },
  dividerText: {
    fontSize: 12,
    color: Colors.dark.text,
    marginHorizontal: 10,
    fontFamily: Fonts.regular,
  },
  inputContainer: {
    marginBottom: 15,
  },
  inputLabel: {
    fontSize: 14,
    color: Colors.dark.text,
    marginBottom: 5,
    fontFamily: Fonts.regular,
  },
  input: {
    backgroundColor: Colors.dark.background,
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 15,
    borderWidth: 1,
    borderColor: Colors.dark.text,
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
  },
  passwordLabelContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  forgotPassword: {
    fontSize: 14,
    color: Colors.light.tint,
    fontFamily: Fonts.regular,
  },
  signInButton: {
    backgroundColor: Colors.light.tint,
    borderRadius: 8,
    paddingVertical: 15,
    alignItems: 'center',
    marginTop: 10,
  },
  signInButtonText: {
    fontSize: 16,
    color: Colors.dark.text,
    fontFamily: Fonts.bold,
  },
  signUpContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 20,
  },
  signUpText: {
    fontSize: 14,
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
  },
  signUpLink: {
    fontSize: 14,
    color: Colors.light.tint,
    fontFamily: Fonts.bold,
  },
});
