package com.example.edgedetectionviewer;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraRenderer";
    
    private MainActivity activity;
    private SurfaceTexture surfaceTexture;
    private int textureId;
    private int shaderProgram;
    
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    
    private int previewWidth = 640;
    private int previewHeight = 480;
    
    private byte[] currentFrameData;
    private boolean frameAvailable = false;
    
    // FPS calculation
    private long lastFrameTime = 0;
    private int frameCount = 0;
    private float currentFPS = 0;
    
    // Vertex shader
    private final String vertexShaderCode =
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_Position = aPosition;" +
            "  vTexCoord = aTexCoord;" +
            "}";
    
    // Fragment shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D uTexture;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
            "}";
    
    // Vertices for a quad
    private static final float[] VERTICES = {
        -1.0f, -1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f,
         1.0f,  1.0f, 0.0f
    };
    
    // Texture coordinates
    private static final float[] TEXTURE_COORDS = {
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    };
    
    public CameraRenderer(MainActivity activity) {
        this.activity = activity;
        
        // Setup buffers
        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);
        
        ByteBuffer tbb = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        textureBuffer = tbb.asFloatBuffer();
        textureBuffer.put(TEXTURE_COORDS);
        textureBuffer.position(0);
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Create texture for camera
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        
        // Create shader program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);
        
        // Create SurfaceTexture
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);
    }
    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        synchronized (this) {
            if (frameAvailable) {
                surfaceTexture.updateTexImage();
                frameAvailable = false;
                
                // Read pixel data and process
                ByteBuffer buffer = ByteBuffer.allocateDirect(previewWidth * previewHeight * 4);
                GLES20.glReadPixels(0, 0, previewWidth, previewHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                
                // Convert to byte array and process
                byte[] data = new byte[previewWidth * previewHeight * 4];
                buffer.get(data);
                
                // Process frame through native code
                byte[] processedData = activity.processFrame(data, previewWidth, previewHeight);
                if (processedData != null) {
                    currentFrameData = processedData;
                    
                    // Upload processed texture
                    ByteBuffer processed = ByteBuffer.wrap(processedData);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, previewWidth, previewHeight, 
                                       0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, processed);
                }
                
                updateFPS();
            }
        }
        
        // Draw quad with texture
        GLES20.glUseProgram(shaderProgram);
        
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        int texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        int textureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }
    
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }
    
    public void onFrameCaptured() {
        // Trigger render
        if (activity != null && activity.findViewById(R.id.glSurfaceView) instanceof GLSurfaceView) {
            ((GLSurfaceView) activity.findViewById(R.id.glSurfaceView)).requestRender();
        }
    }
    
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
    
    private void updateFPS() {
        long currentTime = System.currentTimeMillis();
        frameCount++;
        
        if (currentTime - lastFrameTime >= 1000) {
            currentFPS = frameCount * 1000.0f / (currentTime - lastFrameTime);
            frameCount = 0;
            lastFrameTime = currentTime;
        }
    }
    
    public float getFPS() {
        return currentFPS;
    }
    
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }
    
    public void setPreviewSize(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
    }
    
    public byte[] getCurrentFrameData() {
        return currentFrameData;
    }
}
