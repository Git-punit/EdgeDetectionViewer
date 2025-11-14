# Architecture Deep Dive

## JNI Bridge Flow

### 1. Java to Native
```java
// MainActivity.java
native byte[] nativeProcessFrame(long handle, byte[] data, int width, int height, boolean applyEdgeDetection);
```

### 2. Native Processing
```cpp
// native_processor.cpp
JNIEXPORT jbyteArray JNICALL Java_com_example_edgedetectionviewer_MainActivity_nativeProcessFrame(...) {
    // Convert jbyteArray → C++ byte array
    // Process with OpenCV
    // Convert back to jbyteArray
}
```

## Frame Flow Pipeline

1. **Camera Capture** (Camera2 API)
   - CameraCaptureSession captures to SurfaceTexture
   - Format: YUV → RGBA conversion by hardware

2. **JNI Transfer** (Java → C++)
   - RGBA byte array passed to native code
   - Zero-copy where possible

3. **OpenCV Processing** (C++)
   ```cpp
   Mat rgba → Mat gray → GaussianBlur → Canny → Mat edges
   ```

4. **Texture Upload** (C++ → OpenGL)
   - Processed bytes → RGBA buffer
   - glTexImage2D uploads to GPU

5. **Rendering** (OpenGL ES 2.0)
   - Vertex shader positions quad
   - Fragment shader samples texture
   - Drawn at 60Hz refresh rate

## TypeScript Web Architecture

```typescript
class EdgeDetectionViewer {
    - canvas: HTMLCanvasElement
    - ctx: CanvasRenderingContext2D
    
    + loadFrameFromData(FrameData)
    + renderFrame()
    + updateStats()
}
```

### Data Flow
1. User uploads frame (File API)
2. FileReader converts to base64
3. Image loaded into HTML Image
4. Drawn to Canvas via 2D context
5. Stats extracted and displayed

## Performance Optimizations

1. **Native Code**: All heavy processing in C++
2. **OpenGL Hardware Acceleration**: GPU-based rendering
3. **Minimal Conversions**: Direct RGBA throughout pipeline
4. **Efficient Buffers**: ByteBuffer with native order
5. **Frame Throttling**: Only process when frame available
