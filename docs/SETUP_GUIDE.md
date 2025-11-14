# Quick Setup Guide

## For Evaluators

### 1. Download OpenCV Android SDK

```bash
# Download OpenCV 4.8.1 for Android
wget https://github.com/opencv/opencv/releases/download/4.8.1/opencv-4.8.1-android-sdk.zip
unzip opencv-4.8.1-android-sdk.zip
```

### 2. Set Environment Variable

```bash
export OPENCV_ANDROID_SDK=/path/to/opencv-4.8.1-android-sdk
```

### 3. Open in Android Studio

1. Launch Android Studio
2. Open project: `File → Open → Select EdgeDetectionViewer folder`
3. Wait for Gradle sync
4. Connect Android device or start emulator
5. Click Run (green play button)

### 4. Test Web Viewer

```bash
cd web
npm install
npm run build
npm run serve
```

Open http://localhost:8080 in browser

## Troubleshooting

### OpenCV Not Found
Edit `app/src/main/cpp/CMakeLists.txt` line 10 with your OpenCV path:
```cmake
set(OPENCV_ANDROID_SDK "/your/absolute/path/to/opencv-sdk")
```

### NDK Not Found
Install via Android Studio:
- Tools → SDK Manager → SDK Tools
- Check "NDK (Side by side)"
- Click Apply

### Camera Permission
Grant manually via ADB:
```bash
adb shell pm grant com.example.edgedetectionviewer android.permission.CAMERA
```
