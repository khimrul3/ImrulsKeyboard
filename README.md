# Imruls Keyboard (Android starter project)

An Android Input Method Editor (IME) starter project with:
- English QWERTY keyboard
- Bangla character layout
- Basic Bangla phonetic conversion (starter transliteration rules, **not the full Avro engine**)
- Black theme with rounded colorful keys and dotted lettering
- Android system settings shortcuts to enable/select the keyboard

## Requirements
- Android Studio (current stable)
- Android SDK 35 (the project uses compileSdk 35)
- JDK 17
- Internet access for Gradle to download dependencies on the first build

## Open and build
1. Extract this ZIP.
2. In Android Studio, choose **Open** and select the `ImrulsKeyboard` folder.
3. Let Gradle sync finish.
4. Connect an Android phone with USB debugging enabled, or start an emulator.
5. Press **Run** to install the app.
6. Open Imruls Keyboard, tap **Enable keyboard**, and enable it in Android settings.
7. Tap **Choose keyboard** and select Imruls Keyboard.

To build an APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
The debug APK is usually at `app/build/outputs/apk/debug/app-debug.apk`.

## How to use
- Tap the language/globe key to cycle between English, Bangla phonetic, and Bangla layout.
- In Bangla phonetic mode, type Latin letters; the starter converter replaces recognized letter sequences with Bengali script as you type. Press space to commit the current composition.
- In Bangla layout mode, tap Bengali characters directly.
- Long press / use the `?123` key to switch to the symbols/numbers page.

## Important limitations
- The phonetic converter in this starter is a small rule-based implementation intended to prove the keyboard workflow. It is not a full Avro Phonetic engine and will not match Avro for every word, conjunct, or spelling.
- Voice typing, cloud sync, advanced next-word suggestions, emoji search, and full dictionary-based autocorrect are not implemented in this starter.
- Android controls when a third-party keyboard can appear. Some secure fields may block third-party IMEs.
- Test on your own device before everyday use.
