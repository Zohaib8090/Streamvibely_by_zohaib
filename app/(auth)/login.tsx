
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import { useTheme } from '@react-navigation/native';
import { Link, useRouter } from 'expo-router';
import React, { useState } from 'react';
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
import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../../firebase';
import { useGoogleSignIn } from '../../hooks/useGoogleSignIn';

const Login = () => {
  const { dark } = useTheme();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { signIn: googleSignIn, loading: googleLoading } = useGoogleSignIn();

  const handleLogin = async () => {
    if (email && password) {
      try {
        await signInWithEmailAndPassword(auth, email, password);
        router.replace('/(tabs)');
      } catch (err: any) {
        setError(err.message);
      }
    } else {
      setError('Please enter both email and password.');
    }
  };

  const handleGuestLogin = () => {
    router.replace('/(tabs)');
  };

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
          <TouchableOpacity
            style={styles.googleButton}
            onPress={googleSignIn}
            disabled={googleLoading}>
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
              value={email}
              onChangeText={setEmail}
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
              value={password}
              onChangeText={setPassword}
            />
          </View>

          {error ? <Text style={styles.errorText}>{error}</Text> : null}

          <TouchableOpacity style={styles.signInButton} onPress={handleLogin}>
            <Text style={styles.signInButtonText}>Sign In</Text>
          </TouchableOpacity>
          
          <TouchableOpacity style={styles.guestButton} onPress={handleGuestLogin}>
            <Text style={styles.guestButtonText}>Use without login</Text>
          </TouchableOpacity>

          <View style={styles.signUpContainer}>
            <Text style={styles.signUpText}>Don't have an account?</Text>
            <Link href="/(auth)/signup" asChild>
              <Text style={styles.signUpLink}> Sign up</Text>
            </Link>
          </View>
        </ScrollView>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
};

export default Login;

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
  guestButton: {
    backgroundColor: Colors.dark.background,
    borderRadius: 8,
    paddingVertical: 15,
    alignItems: 'center',
    marginTop: 10,
    borderWidth: 1,
    borderColor: Colors.light.tint,
  },
  guestButtonText: {
    fontSize: 16,
    color: Colors.light.tint,
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
  errorText: {
    color: 'red',
    textAlign: 'center',
    marginBottom: 10,
    fontFamily: Fonts.regular,
  },
});
