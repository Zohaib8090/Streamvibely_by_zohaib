# Permissions and limits

Status: **planned, nothing implemented yet.** The current Android manifest declares **no
permissions**. Permissions are added only when the feature that needs them is built, and each
is requested at the moment of use with a short explanation, never all at launch.

## What the app does that needs access

| Feature | What it needs |
|---|---|
| Play music over the network | Internet |
| Download songs for offline listening | Internet, a place to save files, a notification for progress |
| See songs already on the device (downloaded earlier or by other apps) | Read access to audio files |
| See the songs the app downloaded itself | Nothing extra: the app reads its own files |
| Create and edit playlists | Nothing (stored inside the app) |
| Sign in with YouTube, import and sync playlists | Internet, a Google sign-in, a notification when sync finishes or fails |
| Background playback and media controls | Foreground service, notification |
| Notify the user | Notification permission |

## Android

| Permission | API level | Why | Requested when |
|---|---|---|---|
| `INTERNET` | all | Streaming, lyrics, downloads, sync | Declared at install (no prompt) |
| `ACCESS_NETWORK_STATE` | all | Pick quality by Wi-Fi vs mobile data, pause downloads offline | Declared at install (no prompt) |
| `POST_NOTIFICATIONS` | 33+ | Download progress, sync results, playback controls | First download, first sync, or first playback |
| `FOREGROUND_SERVICE` | all | Keep playing and downloading with the screen off | Declared at install (no prompt) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 34+ | Required type for the playback service | Declared at install (no prompt) |
| `WAKE_LOCK` | all | Keep the CPU awake while playing | Declared at install (no prompt) |
| `READ_MEDIA_AUDIO` | 33+ | List songs already on the device | When the user opens "On this device" |
| `READ_EXTERNAL_STORAGE` (max SDK 32) | 24-32 | Same as above on older Android | Same |

Downloads written by the app:
- On Android 10+ the app saves into its own folder or the shared Music collection through the
  system media store. That needs **no storage permission**.
- On Android 9 and below, `WRITE_EXTERNAL_STORAGE` (max SDK 28) is needed for the shared Music
  folder. Prefer the app's own folder to avoid asking.
- The user may also pick a download folder with the system folder picker (no permission string).

### Inherited by libraries

The built APK also contains `RECEIVE_BOOT_COMPLETED`, merged in by a library (Jetpack Glance
widgets use WorkManager). We did not ask for it. It can be removed from the manifest with
`tools:node="remove"` if widgets prove to work without it.

### The app will not request

`MANAGE_EXTERNAL_STORAGE` (all-files access), `RECORD_AUDIO`, location, contacts, camera,
phone state, accounts, `SYSTEM_ALERT_WINDOW`, install-packages. (The reference app LastWave asks
for `RECORD_AUDIO`; we do not need it unless an audio visualizer is built, and that would be a
separate decision.)

### Google / YouTube sign-in

- Use Google's official sign-in and the **YouTube Data API v3** with the read-only scope to import
  and sync playlists and liked videos. This needs a Google Cloud project, an OAuth consent screen,
  and possibly Google's app verification before real users can sign in.
- Do not ask the user to paste cookies or tokens and do not store their Google password.
- Tokens are kept in encrypted storage (Android Keystore) and removed on sign-out.
- **Status (Build 13):** `androidApp/.../account/GoogleAuthorizer.kt` + `YouTubeAccount.kt` use Google Play services'
  authorization API (see D8 in `docs/decisions.md`). No client ID, secret or redirect is in the app: Google matches the
  Android OAuth client (package `com.zohaibbaig.streamvibely` + signing SHA-1) set up in the `streamvibely` Cloud project.
  The access token is kept in memory only; sign-out forgets the profile. Remove access later at myaccount.google.com/permissions.
- **Cloud project needs:** YouTube Data API v3 enabled; consent screen with the `youtube.readonly` scope; the Android client
  with the right SHA-1. `youtube.readonly` is sensitive: until Google verifies the app, users see an "unverified app" warning.
- **Import:** Account screen lists the user's playlists; importing creates a local playlist from the songs
  already on the device (`importing/TrackMatcher.kt`, title + artist match). Nothing is downloaded from YouTube.
- Syncing is one-way by default: YouTube to the app. Writing changes back to YouTube is a separate
  opt-in feature.

## Web

- No install-time permissions.
- Notifications: the browser asks when the app first calls the Notifications API.
- Local files: File System Access API (Chromium only) when the user picks a folder; otherwise a
  file picker.
- Downloads are saved in browser storage (OPFS or IndexedDB); the user can clear them from the
  browser. Storage persistence may be requested so the browser does not evict them.
- Lock-screen/media keys use the Media Session API (no prompt).

## Linux desktop

- No permission model for normal installs.
- Flatpak (if used) would need explicit holes: network, the Music folder, notifications and the
  media-key/MPRIS D-Bus names. Revisit when packaging.

## Limits we set for ourselves

- Ask for a permission only when the feature is used, explain why in the app first.
- The app must still work, with reduced features, if the user denies notifications or storage.
- Do not read files outside audio media. Do not upload the user's files or library anywhere.
- Downloaded songs stay on the device. Show the user where, and let them delete them all.
- Download only music the user has the right to download. See `docs/providers.md`.

### Notification player (Build 20)

- The player in the notification shade / lock screen needs notifications to be allowed: the permission on Android 13+, and
  the per-app notification switch (Samsung One UI and others can block it separately).
- **Settings > Playback > Notification player** shows whether it is on, asks for the permission, or opens the app's
  notification settings when Android will not show the dialog again (`PlatformActions.notificationsEnabled` / `requestNotifications`).
- Covers of online and downloaded songs are sent to the notification (`MediaMetadata.artworkUri`).
- Samsung battery settings ("Put unused apps to sleep", "Sleeping apps") can also stop background playback: set the app to
  "Unrestricted" under Settings > Apps > Streamvibely > Battery.

### Spotify session cookie (Build 21)

- `spotify/SecureSession.kt` keeps the `sp_dc` cookie encrypted with an Android Keystore AES-GCM key (`spotify_session` prefs,
  key `sp_dc_enc`). A plain value from an older build is encrypted once and the plain copy is removed.
- Disconnect deletes the stored cookie and the login web view's cookies.
- Checked on Waydroid: save, app restart (still connected, so decryption works), Disconnect, restart (signed out).
- Known smell, not changed: the "Paste sp_dc" dialog pre-fills itself with whatever text is on the clipboard.
