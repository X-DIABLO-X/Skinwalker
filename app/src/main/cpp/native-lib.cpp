#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_skinwalker_MainActivity_nativeDiagnostics(
        JNIEnv* env,
        jobject /* this */) {
    std::string diagnostics = "Runtime ready";
    return env->NewStringUTF(diagnostics.c_str());
}
