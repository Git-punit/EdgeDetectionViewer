# Documentation

## Screenshots

Place your app screenshots here:
- `android_screenshot.png` - Android app running with edge detection
- `web_screenshot.png` - Web viewer showing processed frame

## How to Capture Screenshots

### Android App
1. Run app on device/emulator
2. Use Android Studio: View → Tool Windows → Logcat → Screenshot button
3. Or use ADB: `adb exec-out screencap -p > android_screenshot.png`

### Web Viewer
1. Open web viewer in browser
2. Use browser dev tools or screenshot tool
3. Save as `web_screenshot.png`

## GIF Recording

For animated GIFs showing the app in action:

**Android:**
```bash
# Using scrcpy
scrcpy --record android_demo.mp4
# Convert to GIF with ffmpeg
ffmpeg -i android_demo.mp4 -vf "fps=10,scale=320:-1:flags=lanczos" android_demo.gif
```

**Web:**
Use browser extensions like:
- ScreenToGif (Windows)
- Gifox (Mac)
- Peek (Linux)
