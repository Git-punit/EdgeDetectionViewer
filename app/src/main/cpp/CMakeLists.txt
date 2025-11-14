cmake_minimum_required(VERSION 3.22.1)

project("opencv_processing")

# Set C++ standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add OpenCV
# Note: Developer needs to set OPENCV_ANDROID_SDK path or download OpenCV Android SDK
set(OPENCV_ANDROID_SDK "$ENV{OPENCV_ANDROID_SDK}" CACHE PATH "Path to OpenCV Android SDK")

if(NOT OPENCV_ANDROID_SDK)
    message(WARNING "OPENCV_ANDROID_SDK not set. Please set it to your OpenCV Android SDK path")
    # Fallback to common location
    set(OPENCV_ANDROID_SDK "${CMAKE_CURRENT_SOURCE_DIR}/../../../../opencv-sdk")
endif()

set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni")

find_package(OpenCV REQUIRED)

# Add native library
add_library(
    opencv_processing
    SHARED
    native_processor.cpp
)

# Include directories
target_include_directories(
    opencv_processing
    PRIVATE
    ${OpenCV_INCLUDE_DIRS}
)

# Link libraries
target_link_libraries(
    opencv_processing
    ${OpenCV_LIBS}
    android
    log
)
