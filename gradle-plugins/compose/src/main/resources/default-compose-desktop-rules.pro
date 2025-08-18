-keep class kotlin.** { *; }
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }

-assumenosideeffects public class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer,java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer,int,java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
    boolean isTraceInProgress();
    void traceEventStart(int, java.lang.String);
    void traceEventEnd();
}

# Kotlinx Coroutines Rules
# https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# https://youtrack.jetbrains.com/issue/CMP-3818/Update-ProGuard-to-version-7.4-to-support-new-Java-versions
# https://youtrack.jetbrains.com/issue/CMP-7577/Desktop-runRelease-crash-when-upgrade-to-CMP-1.8.0-alpha02
-keep,allowshrinking,allowobfuscation class kotlinx.coroutines.flow.FlowKt** { *; }
-keep,allowshrinking,allowobfuscation class kotlinx.coroutines.Job { *; }
-dontnote kotlinx.coroutines.**

# org.jetbrains.kotlinx:kotlinx-coroutines-swing
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory

# Kotlinx Datetime
#   Material3 depends on it, and it references `kotlinx.serialization`, which is optional
#   Copied from https://github.com/Kotlin/kotlinx-datetime/blob/v0.6.2/core/jvm/resources/META-INF/proguard/datetime.pro
#   with one additional rule
-dontwarn kotlinx.serialization.KSerializer
-dontwarn kotlinx.serialization.Serializable
-dontwarn kotlinx.datetime.serializers.**

# https://github.com/Kotlin/kotlinx.coroutines/issues/2046
-dontwarn android.annotation.SuppressLint

# https://github.com/JetBrains/compose-jb/issues/2393
-dontnote kotlin.coroutines.jvm.internal.**
-dontnote kotlin.internal.**
-dontnote kotlin.jvm.internal.**
-dontnote kotlin.reflect.**
-dontnote kotlinx.coroutines.debug.internal.**
-dontnote kotlinx.coroutines.internal.**
-keep class kotlin.coroutines.Continuation
-keep class kotlinx.coroutines.CancellableContinuation
-keep class kotlinx.coroutines.channels.Channel
-keep class kotlinx.coroutines.CoroutineDispatcher
-keep class kotlinx.coroutines.CoroutineScope
# this is a weird one, but breaks build on some combinations of OS and JDK (reproduced on Windows 10 + Corretto 16)
-dontwarn org.graalvm.compiler.core.aarch64.AArch64NodeMatchRules_MatchStatementSet*

# Androidx
-keep,allowshrinking,allowobfuscation class androidx.compose.runtime.SnapshotStateKt__DerivedStateKt { *; }
-keep class androidx.compose.material3.SliderDefaults { *; }
-dontnote androidx.**

# Kotlinx serialization, included by androidx.navigation
# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$* Companion;
}
-keepnames @kotlinx.serialization.internal.NamedCompanion class *
-if @kotlinx.serialization.internal.NamedCompanion class *
-keepclassmembernames class * {
    static <1> *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# Kotlinx serialization, additional rules

# Fixes:
#   Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'SomeClass' is not found.
#   Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
-keep class **$$serializer {
    *;
}
-dontnote **$$serializer

# Fixes:
#   Exception in thread "main" kotlinx.a.g: Serializer for class 'MyClass' is not found
# When `@InternalSerializationApi kotlinx.serialization.serializer` is used with obfuscation enabled
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$** {
    kotlinx.serialization.KSerializer serializer(...);
}

# org.jetbrains.runtime:jbr-api
-dontwarn com.jetbrains.JBR**
-dontnote com.jetbrains.JBR**
