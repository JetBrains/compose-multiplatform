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

/**
 * A range of instruction offsets that is known to originate from an inlined function.
 */
typedef struct {
    jlocation start_location;
    jlocation end_location;
} InlineRange;

#ifdef DEBUG_ANALYZE_METHOD

void dumpMethod(
  int lineCount, jvmtiLineNumberEntry *lines,
  int variableCount, jvmtiLocalVariableEntry *variables,
  int rangeCount, InlineRange *ranges
) {
    logE("%s", "Analyze Method Lines");

    logE("Local Variable table count=%d", variableCount);
    for (int i=0; i<variableCount; i++) {
        jvmtiLocalVariableEntry *var = &variables[i];
        logE("  %d: start=%lld, length=%d, name=%s, signature=%s, slot=%d",
            i, var->start_location, var->length, var->name, var->signature, var->slot);
    }

    logE("Line Number table count=%d", lineCount);
    for (int i=0; i<lineCount; i++) {
        jvmtiLineNumberEntry *line = &lines[i];
        logE("  %d: start=%lld, line_number=%d",
            i, line->start_location, line->line_number);
    }

    logE("Inline Ranges count=%d", rangeCount);
    for (int i=0; i<rangeCount; i++) {
        InlineRange *range = &ranges[i];
        logE("  %d: start=%lld, end=%lld",
            i, range->start_location, range->end_location);
    }
}
#endif

/**
 * Compute the ranges of inlined instructions from the local variables of a method.
 *
 * The range_ptr must be freed with jvmtiEnv.Deallocate.
 */
void computeInlineRanges(
        jvmtiEnv *jvmti,
        int variableCount,
        jvmtiLocalVariableEntry *variables,
        int *rangeCount_ptr,
        InlineRange **ranges_ptr
) {
    int count = 0;
    InlineRange *ranges = nullptr;
    for (int i=0; i<variableCount; i++) {
        jvmtiLocalVariableEntry *variable = &variables[i];
        if (strncmp("$i$f$", variable->name, 5) == 0) {
            if (ranges == nullptr) {
                jvmti->Allocate(sizeof(InlineRange) * (variableCount-i), (unsigned char **)&ranges);
            }
            ranges[count].start_location = variable->start_location;
            ranges[count].end_location = variable->start_location + variable->length;
            count++;
        }
    }
    *rangeCount_ptr = count;
    *ranges_ptr = ranges;
}

/**
 * Return true if a given line is from an inline function.
 * @param line to investigate
 * @param rangeCount is the number of known inline ranges
 * @param ranges the known inline ranges
 * @return true if the line offset is within one of the inline ranges
 */
bool isInlined(
        jvmtiLineNumberEntry *line,
        int rangeCount,
        InlineRange *ranges
) {
    for (int i=0; i<rangeCount; i++) {
        InlineRange *range = &ranges[i];
        if (range->start_location <= line->start_location &&
            line->start_location < range->end_location) {
            return true;
        }
    }
    return false;
}

/**
 * Analyze the lines of a method to find start and end line excluding inlined functions.
 * @param jvmti the JVMTI environment
 * @param lineCount number of lines found in the method
 * @param lines the actual lines
 * @param variableCount the number of entries of the local variables
 * @param variables the actual variables
 * @param start_line_ptr on return will hold the start line of this method or 0 if not found
 * @param end_line_ptr on return will hold the end line of this method or 0 if not found
 * @return true if a method range is found
 */
bool analyzeLines(
        jvmtiEnv *jvmti,
        int lineCount, jvmtiLineNumberEntry *lines,
        int variableCount, jvmtiLocalVariableEntry *variables,
        int *start_line_ptr,
        int *end_line_ptr
) {
    int rangeCount = 0;
    InlineRange *ranges = nullptr;
    computeInlineRanges(jvmti, variableCount, variables, &rangeCount, &ranges);
    int start_line = 0;
    int end_line = 0;

#ifdef DEBUG_ANALYZE_METHOD
    dumpMethod(lineCount, lines, variableCount, variables, rangeCount, ranges);
#endif

    for (int i=0; i<lineCount; i++) {
        jvmtiLineNumberEntry *line = &lines[i];
        int line_number = line->line_number;
        if (line_number > 0 && !isInlined(line, rangeCount, ranges)) {
            if (start_line == 0) {
                start_line = line_number;
                end_line = line_number;
            }
            else if (line_number < start_line) {
                start_line = line_number;
            }
            else if (line_number > end_line) {
                end_line = line_number;
            }
        }
    }
    jvmti->Deallocate((unsigned char *)ranges);
    *start_line_ptr = start_line;
    *end_line_ptr = end_line;
    return start_line > 0;
}

/**
 * Deallocate the local variables and any allocations held by a local variable entry
 * @param jvmti the JVMTI environment
 * @param variableCount the number of entries of the local variables
 * @param variables_ptr a reference to the actual variables
 */
void deallocateVariables(
        jvmtiEnv *jvmti,
        int variableCount,
        jvmtiLocalVariableEntry **variables_ptr
) {
    jvmtiLocalVariableEntry *variables = *variables_ptr;
    if (variables != nullptr) {
        for (int i=0; i<variableCount; i++) {
            jvmtiLocalVariableEntry *entry = &variables[i];
            jvmti->Deallocate((unsigned char *)entry->name);
            jvmti->Deallocate((unsigned char *)entry->signature);
            jvmti->Deallocate((unsigned char *)entry->generic_signature);
        }
        jvmti->Deallocate((unsigned char *)variables);
    }
    *variables_ptr = nullptr;
}

void deallocateLines(jvmtiEnv *jvmti, jvmtiLineNumberEntry **lines_ptr) {
    jvmti->Deallocate((unsigned char *)*lines_ptr);
    *lines_ptr = nullptr;
}

const int ACC_BRIDGE = 0x40;

jobject resolveLocation(JNIEnv *env, jclass lambda_class) {
    if (!create_lambda_location_result_fields(env)) {
        return nullptr;
    }
    jvmtiEnv *jvmti = getJvmti(env);
    if (jvmti == nullptr) {
        return nullptr;
    }
    int methodCount;
    jmethodID *methods;
    jvmtiError error = jvmti->GetClassMethods(lambda_class, &methodCount, &methods);
    if (CheckJvmtiError(jvmti, error)) {
        return nullptr;
    }

    int variableCount = 0;
    jvmtiLocalVariableEntry *variables = nullptr;
    int lineCount = 0;
    jvmtiLineNumberEntry *lines = nullptr;
    int start_line = 0;
    int end_line = 0;

    for (int i = 0; i < methodCount; i++) {
        deallocateLines(jvmti, &lines);
        deallocateVariables(jvmti, variableCount, &variables);

        jmethodID methodId = methods[i];
        int modifiers = 0;
        error = jvmti->GetMethodModifiers(methodId, &modifiers);
        if (CheckJvmtiError(jvmti, error)) {
            break;
        }
        if ((modifiers & ACC_BRIDGE) != 0) { // NOLINT(hicpp-signed-bitwise)
            continue; // Ignore bridge methods
        }

        char* name;
        error = jvmti->GetMethodName(methodId, &name, nullptr, nullptr);
        if (CheckJvmtiError(jvmti, error)) {
            break;
        }
        bool isInvokeMethod = strcmp(name, "invoke") == 0;
        jvmti->Deallocate((unsigned char *)name);
        if (!isInvokeMethod) {
            continue; // Ignore if the method name doesn't match "invoke"
        }

        error = jvmti->GetLocalVariableTable(methodId, &variableCount, &variables);
        if (CheckJvmtiError(jvmti, error)) {
            break;
        }
        error = jvmti->GetLineNumberTable(methodId, &lineCount, &lines);
        if (CheckJvmtiError(jvmti, error)) {
            break;
        }

        if (analyzeLines(jvmti, lineCount, lines, variableCount, variables,
                         &start_line, &end_line)) {
            break;
        }
    }
    deallocateLines(jvmti, &lines);
    deallocateVariables(jvmti, variableCount, &variables);
    jvmti->Deallocate((unsigned char *)methods);

    if (start_line <= 0) {
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