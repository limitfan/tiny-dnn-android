#include <jni.h>
#include <string>
#include <algorithm>
#include <vector>
#include <memory>
#include <streambuf>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "tiny_dnn/tiny_dnn.h"
using namespace tiny_dnn;
using namespace tiny_dnn::activation;
using namespace tiny_dnn::layers;

#define APPNAME "TINYDNNANDROID"


#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,APPNAME,__VA_ARGS__)

// rescale output to 0-100
template <typename Activation>
double rescale(double x) {
    Activation a;
    return 100.0 * (x - a.scale().first) / (a.scale().second - a.scale().first);
}

struct membuf : std::streambuf
{
    membuf(char* begin, char* end) {
        this->setg(begin, begin, end);
    }
};


network<sequential> nn;
void convert_image(const std::string& imagefilename,
                   double minv,
                   double maxv,
                   int w,
                   int h,
                   vec_t& data) {

    image<> img(imagefilename, image_type::grayscale);
    image<> resized = resize_image(img, w, h);

    // mnist dataset is "white on black", so negate required
    std::transform(resized.begin(), resized.end(), std::back_inserter(data),
                   [=](uint8_t c) { return (255 - c) * (maxv - minv) / 255.0 + minv; });
}

float ret[10];

float* recognize(const std::string& dictionary, const std::string& src_filename) {
    // convert imagefile to vec_t
    vec_t data;
    convert_image(src_filename, -1.0, 1.0, 32, 32, data);

    // recognize
    auto res = nn.predict(data);
    for (int i = 0; i < 10; i++) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "predicting results:%d %lf\n", i,
                            rescale<tan_h>(res[i]));

       // ret[i] = rescale<tan_h>(res[i]);
        ret[i] = rescale<tan_h>(res[i]);
    }
    return ret;
    // save outputs of each layer
}

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


float* recognize(const std::string& dictionary, jfloat* digitsdata) {




    // convert imagefile to vec_t
    vec_t data;
    for(int i = 0; i<32*32;++i){
        data.push_back(digitsdata[i]);
    }


    // recognize
    auto res = nn.predict(data);

    for (int i = 0; i < 10; i++) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "predicting results:%d %lf\n", i,
                            rescale<tan_h>(res[i]));

        ret[i] = rescale<tan_h>(res[i]);
    }
    return ret;

}



extern "C" {

void  Java_com_tinydnn_android_MainActivity_loadModel(JNIEnv* env,jclass tis
        ,jobject assetManager,jstring filename)
{
    LOGI("ReadAssets");
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if(mgr==NULL)
    {
        LOGI(" %s","AAssetManager==NULL");
        return ;
    }

    jboolean iscopy;
    const char *mfile = env->GetStringUTFChars(filename, &iscopy);
    AAsset* asset = AAssetManager_open(mgr, mfile,AASSET_MODE_UNKNOWN);
    env->ReleaseStringUTFChars(filename, mfile);
    if(asset==NULL)
    {
        LOGI(" %s","asset==NULL");
        return ;
    }


    off_t bufferSize = AAsset_getLength(asset);
    LOGI("file size         : %d\n",bufferSize);
    char *buffer=(char *)malloc(bufferSize+1);
    buffer[bufferSize]=0;
    int numBytesRead = AAsset_read(asset, buffer, bufferSize);

    membuf sbuf(buffer, buffer + bufferSize);
    std::istream in(&sbuf);

    nn.load(in,  content_type::weights_and_model, file_format::binary);
    //nn.load("/sdcard/LeNet-model");
   // LOGI(": %s",buffer);
    free(buffer);
    AAsset_close(asset);
}

jstring
Java_com_tinydnn_android_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    double elapsed = benchmark();
    std::string hello = "AlexNet benchmark elapsed time(s):" + std::to_string(elapsed);
    return env->NewStringUTF(hello.c_str());
}
jfloatArray JNICALL Java_com_tinydnn_android_MainActivity_recognize
        (JNIEnv *env, jobject obj, jfloatArray fltarray1)
{

    jfloatArray result;
    result = env->NewFloatArray(10);
    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }

    jfloat array1[10];
    jfloat* flt1 = env->GetFloatArrayElements(fltarray1,0);

    //float *current = recognize("/sdcard/LeNet-model", flt1);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "before recognizing bitmap in sdcard");

    float * current = recognize("/sdcard/LeNet-model", "/sdcard/data/digit.jpg");
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "after recognizing bitmap in sdcard");

    for(int i = 0;i<10;++i) {
        array1[i] = current[i];
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "ret:%d %lf\n", i,
                            array1[i]);

    }

    env->ReleaseFloatArrayElements(fltarray1, flt1, 0);
    env->SetFloatArrayRegion(result, 0, 10, array1);
    return result;

}

}