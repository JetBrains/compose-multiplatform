#include <jni.h>
#include <jvmti.h>
#include <android/log.h>
#include <string>

namespace compose_inspection {

void logE(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_ERROR, "ComposeLayoutInspector", fmt, args);
    va_end(args);
}

bool CheckJvmtiError(jvmtiEnv *jvmti, jvmtiError err_num) {
    if (err_num == JVMTI_ERROR_NONE) {
        return false;
    }

    char *error = nullptr;
    jvmti->GetErrorName(err_num, &error);
    logE("JVMTI error: %d(%s)", err_num, error == nullptr ? "Unknown" : error);
    if (error != nullptr) {
        jvmti->Deallocate((unsigned char *) error);
    }

    return true;
}

void SetAllCapabilities(jvmtiEnv *jvmti) {
    jvmtiCapabilities caps;
    jvmtiError error;
    error = jvmti->GetPotentialCapabilities(&caps);
    CheckJvmtiError(jvmti, error);
    error = jvmti->AddCapabilities(&caps);
    CheckJvmtiError(jvmti, error);
}

jvmtiEnv *CreateJvmtiEnv(JavaVM *vm) {
    jvmtiEnv *jvmti_env;
    jint jvmti_flag = JVMTI_VERSION_1_2;
    jint result = vm->GetEnv((void **) &jvmti_env, jvmti_flag);
    if (result != JNI_OK) {
        return nullptr;
    }

    return jvmti_env;
}

static jclass location_class = nullptr;
static jmethodID location_class_constructor = nullptr;

bool create_lambda_location_result_fields(JNIEnv *env) {
    static bool failure_creating_result = false;

    if (failure_creating_result) {
        return false;
    }
    if (location_class != nullptr) {
        return true;
    }
    failure_creating_result = true;  // In case the next lines throw exceptions...
    jclass local_location_class =
            env->FindClass("androidx/compose/ui/inspection/LambdaLocation");
    location_class = (jclass) env->NewGlobalRef(local_location_class);
    location_class_constructor =
            env->GetMethodID(location_class, "<init>", "(Ljava/lang/String;II)V");
    if (location_class == nullptr || location_class_constructor == nullptr) {
        return false;
    }
    failure_creating_result = false;
    return true;
}

/**
 * Create a Jvmti environment.
 * The env is created and kept in a static for the duration of the JVM lifetime.
 * Check if we are able to get the can_get_line_numbers capability. If not just
 * return nullptr now and next time this is called.
 */
jvmtiEnv *getJvmti(JNIEnv *env) {
    static jvmtiEnv *jvmti_env = nullptr;
    static bool can_get_line_numbers = true;
    if (jvmti_env != nullptr || !can_get_line_numbers) {
        return jvmti_env;
    }
    can_get_line_numbers = false;
    JavaVM *vm;
    int error = env->GetJavaVM(&vm);
    if (error != 0) {
        logE("Failed to get JavaVM instance for LayoutInspector with error "
             "code: %d",
             error);
        return nullptr;
    }
    // Create a stand-alone jvmtiEnv to avoid any callback conflicts
    // with other profilers' agents.
    jvmti_env = CreateJvmtiEnv(vm);
    if (jvmti_env == nullptr) {
        logE("Failed to initialize JVMTI env for LayoutInspector");
    } else {
        SetAllCapabilities(jvmti_env);
        jvmtiCapabilities capabilities;
        if (!CheckJvmtiError(jvmti_env,
                             jvmti_env->GetCapabilities(&capabilities))) {
            can_get_line_numbers = capabilities.can_get_line_numbers;
        }
        if (!can_get_line_numbers) {
            logE("Failed to get the can_get_line_numbers capability for JVMTI");
            jvmti_env = nullptr;
        }
    }
    return jvmti_env;
}

jobject resolveLocation(JNIEnv *env, jclass lambda_class) {
    if (!create_lambda_location_result_fields(env)) {
        return nullptr;
    }
    jvmtiEnv *jvmti = getJvmti(env);
    if (jvmti == nullptr) {
        return nullptr;
    }
    jint count;
    jmethodID *methods;
    jvmtiError error = jvmti->GetClassMethods(lambda_class, &count, &methods);
    if (CheckJvmtiError(jvmti, error)) {
        return nullptr;
    }
    jint start_line = -1;
    jint end_line = -1;
    for (int i = count - 1; i >= 0; i--) {
        jint entries;
        jvmtiLineNumberEntry *table;
        error = jvmti->GetLineNumberTable(methods[i], &entries, &table);
        if (CheckJvmtiError(jvmti, error)) {
            continue;
        }
        if (entries > 0) {
            start_line = table[0].line_number;
            end_line = table[entries - 1].line_number;
        }
        jvmti->Deallocate((unsigned char *) table);
        if (start_line > 0 && end_line > 0 && end_line >= start_line) {
            break;
        }
    }
    jvmti->Deallocate((unsigned char *) methods);

    if (start_line < 0 || end_line < 0) {
        return nullptr;
    }

    char *source_name_ptr;
    error = jvmti->GetSourceFileName(lambda_class, &source_name_ptr);
    if (CheckJvmtiError(jvmti, error)) {
        return nullptr;
    }
    jstring file_name = env->NewStringUTF(source_name_ptr);
    jvmti->Deallocate((unsigned char *) source_name_ptr);
    jobject result = env->NewObject(location_class, location_class_constructor,
                                    file_name, start_line, end_line);
    return result;
}
} // namespace compose_inspection

extern "C" {

JNIEXPORT jobject JNICALL
Java_androidx_compose_ui_inspection_LambdaLocation_resolve(
        JNIEnv *env, __unused jclass clazz, jclass lambda_class) {
    return compose_inspection::resolveLocation(env, lambda_class);
}

}