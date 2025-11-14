#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <vector>

#define TAG "NativeProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace cv;

/**
 * Native processor class to handle OpenCV operations
 */
class NativeProcessor {
public:
    NativeProcessor() {
        LOGD("NativeProcessor created");
    }
    
    ~NativeProcessor() {
        LOGD("NativeProcessor destroyed");
    }
    
    /**
     * Process frame with edge detection or grayscale
     */
    std::vector<uint8_t> processFrame(const uint8_t* data, int width, int height, bool applyEdgeDetection) {
        try {
            // Create Mat from RGBA data
            Mat rgba(height, width, CV_8UC4, (void*)data);
            Mat processed;
            
            if (applyEdgeDetection) {
                // Convert to grayscale
                Mat gray;
                cvtColor(rgba, gray, COLOR_RGBA2GRAY);
                
                // Apply Gaussian blur to reduce noise
                Mat blurred;
                GaussianBlur(gray, blurred, Size(5, 5), 1.5);
                
                // Apply Canny edge detection
                Mat edges;
                Canny(blurred, edges, 50, 150);
                
                // Convert back to RGBA for display
                cvtColor(edges, processed, COLOR_GRAY2RGBA);
                
                LOGD("Processed frame with edge detection: %dx%d", width, height);
            } else {
                // Return grayscale version
                Mat gray;
                cvtColor(rgba, gray, COLOR_RGBA2GRAY);
                cvtColor(gray, processed, COLOR_GRAY2RGBA);
                
                LOGD("Processed frame with grayscale: %dx%d", width, height);
            }
            
            // Convert Mat to vector
            std::vector<uint8_t> result;
            result.assign(processed.data, processed.data + processed.total() * processed.channels());
            
            return result;
            
        } catch (const cv::Exception& e) {
            LOGE("OpenCV exception: %s", e.what());
            // Return original data on error
            std::vector<uint8_t> result(data, data + (width * height * 4));
            return result;
        }
    }
};

// JNI method implementations

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_edgedetectionviewer_MainActivity_nativeCreate(JNIEnv* env, jobject thiz) {
    NativeProcessor* processor = new NativeProcessor();
    return reinterpret_cast<jlong>(processor);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_edgedetectionviewer_MainActivity_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle != 0) {
        NativeProcessor* processor = reinterpret_cast<NativeProcessor*>(handle);
        delete processor;
    }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_edgedetectionviewer_MainActivity_nativeProcessFrame(
    JNIEnv* env, 
    jobject thiz, 
    jlong handle, 
    jbyteArray data, 
    jint width, 
    jint height, 
    jboolean applyEdgeDetection
) {
    if (handle == 0) {
        LOGE("Invalid native handle");
        return nullptr;
    }
    
    NativeProcessor* processor = reinterpret_cast<NativeProcessor*>(handle);
    
    // Get input data
    jbyte* inputData = env->GetByteArrayElements(data, nullptr);
    jsize inputSize = env->GetArrayLength(data);
    
    // Process frame
    std::vector<uint8_t> result = processor->processFrame(
        reinterpret_cast<uint8_t*>(inputData), 
        width, 
        height, 
        applyEdgeDetection
    );
    
    // Release input data
    env->ReleaseByteArrayElements(data, inputData, JNI_ABORT);
    
    // Create output array
    jbyteArray outputArray = env->NewByteArray(result.size());
    env->SetByteArrayRegion(outputArray, 0, result.size(), reinterpret_cast<jbyte*>(result.data()));
    
    return outputArray;
}
