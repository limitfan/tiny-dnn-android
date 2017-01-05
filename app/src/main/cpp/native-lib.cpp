#include <jni.h>
#include <string>
#include <android/log.h>
#include "tiny_dnn/tiny_dnn.h"
using namespace tiny_dnn;

#define APPNAME "TINYDNNANDROID"

static double now_ms(void) {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
}

static double benchmark(){
    models::alexnet nn;

    // change all layers at once
    nn.weight_init(weight_init::constant(2.0));
    nn.bias_init(weight_init::constant(2.0));
    nn.init_weight();

    vec_t in(224 * 224 * 3);

    // generate random variables
    uniform_rand(in.begin(), in.end(), 0, 1);

    timer t; // start the timer

    // predict
    auto res = nn.predict(in);

    double elapsed_ms = t.elapsed();
    t.stop();

    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "AlexNet benchmark elapsed time(s): %lf\n", elapsed_ms );

    return elapsed_ms;
}

extern "C"
jstring
Java_com_tinydnn_android_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    double elapsed = benchmark();
    std::string hello = "AlexNet benchmark elapsed time(s):"+std::to_string(elapsed);
    return env->NewStringUTF(hello.c_str());
}
