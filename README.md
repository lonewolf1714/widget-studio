# Widget Studio

An Android home-screen widget app (package `com.widgetstudio`). Every widget is drawn as a bitmap
with RemoteViews, so it looks the same on any launcher. No AndroidX, no Gradle, no third-party code.

## Widgets

Calendar, Events, Storage, Big Date, Quote (built-in English quotes and shayari that change every
6 hours or every day; tap for the next one), Counter, iPod Music, Games (coin flip, dice,
tic-tac-toe, Lucky 7), Year in Dots, Custom Combo, Weather, Photo, Clock and App List. Each one has
an editor with themes, fonts, colours and layout options.

## Build and install

```
python -I build.py      # -> WidgetStudio.apk (aapt2 -> javac -> d8 -> zipalign -> apksigner)
adb install -r --user 0 WidgetStudio.apk
```

Needs `%LOCALAPPDATA%\Android\Sdk` with build-tools 35 and platforms/android-35, plus a JDK on PATH.
The build signs with a local `debug.keystore`. It isn't in this repo; `build.py` creates a new one
the first time you build.

Fonts in `res/font` are from Google Fonts (SIL Open Font License).
