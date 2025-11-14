package com.example.edgedetectionviewer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    
    private GLSurfaceView glSurfaceView;
    private CameraRenderer renderer;
    private TextView fpsText;
    private TextView resolutionText;
    private Button toggleButton;
    private Button saveButton;
    
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    private Size previewSize;
    private boolean isProcessingEnabled = true;
    
    // Native methods
    static {
        System.loadLibrary("opencv_processing");
    }
    
    private native long nativeCreate();
    private native void nativeDestroy(long handle);
    private native byte[] nativeProcessFrame(long handle, byte[] data, int width, int height, boolean applyEdgeDetection);
    
    private long nativeHandle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize native
        nativeHandle = nativeCreate();
        
        // Setup views
        glSurfaceView = findViewById(R.id.glSurfaceView);
        fpsText = findViewById(R.id.fpsText);
        resolutionText = findViewById(R.id.resolutionText);
        toggleButton = findViewById(R.id.toggleButton);
        saveButton = findViewById(R.id.saveButton);
        
        // Setup OpenGL
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new CameraRenderer(this);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        // Setup buttons
        toggleButton.setOnClickListener(v -> {
            isProcessingEnabled = !isProcessingEnabled;
            toggleButton.setText(isProcessingEnabled ? "Show Raw" : "Show Edges");
            Toast.makeText(this, isProcessingEnabled ? "Edge detection ON" : "Raw feed", Toast.LENGTH_SHORT).show();
        });
        
        saveButton.setOnClickListener(v -> saveCurrentFrame());
        
        // Setup FPS updater
        Handler mainHandler = new Handler();
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                float fps = renderer.getFPS();
                fpsText.setText(String.format("FPS: %.1f", fps));
                if (previewSize != null) {
                    resolutionText.setText(String.format("Resolution: %dx%d", previewSize.getWidth(), previewSize.getHeight()));
                }
                mainHandler.postDelayed(this, 500);
            }
        }, 500);
        
        // Request camera permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
        } else {
            setupCamera();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void setupCamera() {
        startBackgroundThread();
        
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            
            // Choose preview size (640x480 for performance)
            Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
            previewSize = chooseOptimalSize(sizes);
            
            renderer.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        }
    }
    
    private Size chooseOptimalSize(Size[] sizes) {
        // Prefer 640x480 for performance
        for (Size size : sizes) {
            if (size.getWidth() == 640 && size.getHeight() == 480) {
                return size;
            }
        }
        // Fallback to first available
        return sizes[0];
    }
    
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }
        
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }
        
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };
    
    private void createCameraPreview() {
        try {
            SurfaceTexture texture = renderer.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            
            final CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(surface);
            
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    
                    requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    
                    try {
                        captureSession.setRepeatingRequest(requestBuilder.build(), captureCallback, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start camera preview", e);
                    }
                }
                
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed");
                }
            }, backgroundHandler);
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
        }
    }
    
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            // Frame captured - process via renderer
            renderer.onFrameCaptured();
        }
    };
    
    public byte[] processFrame(byte[] data, int width, int height) {
        if (nativeHandle != 0) {
            return nativeProcessFrame(nativeHandle, data, width, height, isProcessingEnabled);
        }
        return data;
    }
    
    private void saveCurrentFrame() {
        byte[] frameData = renderer.getCurrentFrameData();
        if (frameData != null) {
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EdgeDetection");
                if (!dir.exists()) dir.mkdirs();
                
                File file = new File(dir, "frame_" + System.currentTimeMillis() + ".raw");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(frameData);
                fos.close();
                
                Toast.makeText(this, "Frame saved: " + file.getName(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save frame", e);
                Toast.makeText(this, "Failed to save frame", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Background thread interrupted", e);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }
    
    @Override
    protected void onPause() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        stopBackgroundThread();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        if (nativeHandle != 0) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
        super.onDestroy();
    }
}
