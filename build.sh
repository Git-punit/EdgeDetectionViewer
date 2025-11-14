#!/bin/bash

# Build script for Edge Detection Viewer
# This script builds both Android app and TypeScript web viewer

set -e

echo "========================================="
echo "Edge Detection Viewer - Build Script"
echo "========================================="

# Check if OpenCV is set
if [ -z "$OPENCV_ANDROID_SDK" ]; then
    echo "⚠️  WARNING: OPENCV_ANDROID_SDK environment variable not set"
    echo "Please set it to your OpenCV Android SDK path:"
    echo "  export OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Build Android App
echo ""
echo "📱 Building Android App..."
echo "-------------------------------------------"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew clean
    ./gradlew assembleDebug
    echo "✅ Android APK built successfully!"
    echo "📦 Output: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ gradlew not found. Please run from project root."
    exit 1
fi

# Build Web Viewer
echo ""
echo "🌐 Building Web Viewer..."
echo "-------------------------------------------"
if [ -d "web" ]; then
    cd web
    
    if ! command -v npm &> /dev/null; then
        echo "⚠️  npm not found. Skipping web build."
    else
        npm install
        npm run build
        echo "✅ Web viewer built successfully!"
        echo "📁 Output: web/dist/"
        echo "🚀 To serve: cd web && npm run serve"
    fi
    
    cd ..
else
    echo "❌ web directory not found"
fi

echo ""
echo "========================================="
echo "✅ Build Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Install APK: adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo "2. Run web viewer: cd web && npm run serve"
echo ""
