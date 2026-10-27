# Imruls Keyboard (Android)

English-only Android keyboard starter with a black background, rounded multicolor keys, a compact toolbar, a symbol layer, and responsive weighted rows. The main app screen provides buttons to enable and select the keyboard.

## Build

- Android Studio with JDK 17 and Android SDK 35, or GitHub Actions workflow in `.github/workflows/android-apk.yml`.
- Build debug APK with `gradle :app:assembleDebug`.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Enable on Android

1. Install the APK.
2. Open Imruls Keyboard and tap **Enable keyboard**.
3. Enable **Imruls Keyboard** in system settings.
4. Return to the app and tap **Choose keyboard**, then select Imruls Keyboard.

This is a starter keyboard. It does not include cloud sync, voice typing, predictive suggestions, or the full Avro engine. Test on your own device before relying on it.
