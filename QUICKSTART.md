# ⚡ Quick Start Guide

## For Evaluators - 5 Minute Setup

### Step 1: Download OpenCV (2 minutes)
```bash
wget https://github.com/opencv/opencv/releases/download/4.8.1/opencv-4.8.1-android-sdk.zip
unzip opencv-4.8.1-android-sdk.zip
export OPENCV_ANDROID_SDK=$PWD/opencv-4.8.1-android-sdk
```

### Step 2: Open in Android Studio (1 minute)
1. Launch Android Studio
2. File → Open → Select `EdgeDetectionViewer` folder
3. Wait for Gradle sync

### Step 3: Build & Run (2 minutes)
1. Connect Android device (or start emulator)
2. Click green "Run" button
3. Grant camera permission when prompted

### Step 4: Test Web Viewer (optional)
```bash
cd web
npm install && npm run build && npm run serve
# Open http://localhost:8080
```

## Expected Results

✅ Camera opens automatically  
✅ Real-time edge detection visible  
✅ FPS counter shows 10-30 FPS  
✅ Toggle button switches modes  
✅ Save button exports frames

## Troubleshooting

**Build Fails**: Set OpenCV path in `app/src/main/cpp/CMakeLists.txt` line 10  
**No Camera**: Grant permission via `adb shell pm grant com.example.edgedetectionviewer android.permission.CAMERA`  
**Low FPS**: Normal on emulator. Use physical device for better performance.

## Files to Review

| File | Purpose |
|------|---------|
| `MainActivity.java` | Camera setup, UI, JNI calls |
| `CameraRenderer.java` | OpenGL rendering logic |
| `native_processor.cpp` | OpenCV edge detection |
| `web/src/viewer.ts` | TypeScript web interface |
| `README.md` | Complete documentation |

## Git History
```bash
git log --oneline
# Shows 8 meaningful commits with clear progression
```

## Contact
Questions? Create a GitHub issue or email intern@example.com
