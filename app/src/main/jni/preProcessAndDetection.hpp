#include <jni.h>
#include <stdlib.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <queue>
#include <fstream>
#include <string>
#include <algorithm>
#include <sys/types.h>
#include <sys/stat.h>

//#define  LOG_TAG    "OCV:ling_native"
#define  LOG_TAG    "cfws"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C" {
JNIEXPORT void JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_CreateCascadeClassifier(JNIEnv *env, jobject obj, jstring jCascadeFilePath);
JNIEXPORT jintArray JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_preProcessAndDetection(JNIEnv *env, jobject obj, jstring fnt, jlong imgAddr, jstring jCascadeFilePath);
//JNIEXPORT void JNICALL Java_com_seu_SecureFingerMouse_OpenCVWorker_preSkinFilter(JNIEnv* env, jobject obj, jlong imgAddr);
}
