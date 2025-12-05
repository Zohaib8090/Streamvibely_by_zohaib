import * as React from 'react';
import * as WebBrowser from 'expo-web-browser';
import * as Google from 'expo-auth-session/providers/google';
import { GoogleAuthProvider, signInWithCredential } from 'firebase/auth';
import { auth } from '../firebase';
import { useRouter } from 'expo-router';

WebBrowser.maybeCompleteAuthSession();

export const useGoogleSignIn = () => {
  const router = useRouter();
  const [request, response, promptAsync] = Google.useIdTokenAuthRequest({
    clientId: '731940880209-n8sgjjl15u3eoqogb2qj02u1gjm4psh9.apps.googleusercontent.com',
  });

  React.useEffect(() => {
    const handleResponse = async () => {
      if (response?.type === 'success') {
        const { id_token } = response.params;
        const credential = GoogleAuthProvider.credential(id_token);
        try {
          await signInWithCredential(auth, credential);
          router.replace('/(tabs)');
        } catch (error) {
          console.error("Firebase sign-in error:", error);
        } finally {
          WebBrowser.dismissBrowser();
        }
      } else {
        WebBrowser.dismissBrowser();
      }
    };
    handleResponse();
  }, [response, router]);

  const signIn = () => {
    promptAsync();
  };

  return { signIn, loading: !request };
};
