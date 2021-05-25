/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.dependencies

import androidx.build.OperatingSystem
import androidx.build.getOperatingSystem

const val ANDROIDX_TEST_VERSION = "1.3.0"
const val ANDROIDX_TEST_CORE = "androidx.test:core:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_EXT_JUNIT = "androidx.test.ext:junit:1.1.2"
const val ANDROIDX_TEST_EXT_KTX = "androidx.test.ext:junit-ktx:1.1.2"
const val ANDROIDX_TEST_EXT_TRUTH = "androidx.test.ext:truth:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_MONITOR = "androidx.test:monitor:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_RULES = "androidx.test:rules:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_RUNNER = "androidx.test:runner:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_UIAUTOMATOR = "androidx.test.uiautomator:uiautomator:2.2.0"

// Remove ANDROIDX_TEST_*_LATEST once ANDROIDX_TEST_* upgrades to the next stable release
// after 1.3.0, and replace all usages of ANDROIDX_TEST_*_LATEST with ANDROIDX_TEST_*
const val ANDROIDX_TEST_LATEST_VERSION = "1.4.0-alpha06"
const val ANDROIDX_TEST_CORE_LATEST = "androidx.test:core:$ANDROIDX_TEST_LATEST_VERSION"
const val ANDROIDX_TEST_EXT_TRUTH_LATEST = "androidx.test.ext:truth:$ANDROIDX_TEST_LATEST_VERSION"
const val ANDROIDX_TEST_MONITOR_LATEST = "androidx.test:monitor:$ANDROIDX_TEST_LATEST_VERSION"
const val ANDROIDX_TEST_RULES_LATEST = "androidx.test:rules:$ANDROIDX_TEST_LATEST_VERSION"
const val ANDROIDX_TEST_RUNNER_LATEST = "androidx.test:runner:$ANDROIDX_TEST_LATEST_VERSION"

const val AUTO_COMMON = "com.google.auto:auto-common:0.11"
const val AUTO_SERVICE_ANNOTATIONS = "com.google.auto.service:auto-service-annotations:1.0-rc6"
const val AUTO_SERVICE_PROCESSOR = "com.google.auto.service:auto-service:1.0-rc6"
const val AUTO_VALUE = "com.google.auto.value:auto-value:1.6.3"
const val AUTO_VALUE_ANNOTATIONS = "com.google.auto.value:auto-value-annotations:1.6.3"
const val AUTO_VALUE_PARCEL = "com.ryanharter.auto.value:auto-value-parcel:0.2.6"
const val ANTLR = "org.antlr:antlr4:4.7.1"
const val APACHE_COMMONS_CODEC = "commons-codec:commons-codec:1.10"
const val ASSERTJ = "org.assertj:assertj-core:3.11.1"
const val CHECKER_FRAMEWORK = "org.checkerframework:checker-qual:2.5.3"
const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:2.0.1@aar"
const val CONSTRAINT_LAYOUT_CORE = "androidx.constraintlayout:constraintlayout-core:1.0.0-alpha1"
const val DAGGER = "com.google.dagger:dagger:2.35"
const val DAGGER_COMPILER = "com.google.dagger:dagger-compiler:2.35"
const val DEXMAKER_MOCKITO = "com.linkedin.dexmaker:dexmaker-mockito:2.25.0"
const val DEXMAKER_MOCKITO_INLINE = "com.linkedin.dexmaker:dexmaker-mockito-inline:2.25.0"
const val ESPRESSO_CONTRIB = "androidx.test.espresso:espresso-contrib:3.3.0"
const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:3.3.0"
const val ESPRESSO_INTENTS = "androidx.test.espresso:espresso-intents:3.3.0"
const val ESPRESSO_IDLING_NET = "androidx.test.espresso.idling:idling-net:3.3.0"
const val ESPRESSO_IDLING_RESOURCE = "androidx.test.espresso:espresso-idling-resource:3.3.0"
const val ESPRESSO_WEB = "androidx.test.espresso:espresso-web:3.3.0"
const val FINDBUGS = "com.google.code.findbugs:jsr305:3.0.2"
const val FIREBASE_APPINDEXING = "com.google.firebase:firebase-appindexing:19.2.0"
const val GCM_NETWORK_MANAGER = "com.google.android.gms:play-services-gcm:17.0.0"
const val GOOGLE_COMPILE_TESTING = "com.google.testing.compile:compile-testing:0.18"
const val GSON = "com.google.code.gson:gson:2.8.0"
const val GUAVA_VERSION = "29.0-jre"
const val GUAVA = "com.google.guava:guava:$GUAVA_VERSION"
const val GUAVA_ANDROID_VERSION = "29.0-android"
const val GUAVA_ANDROID = "com.google.guava:guava:$GUAVA_ANDROID_VERSION"
const val GUAVA_LISTENABLE_FUTURE = "com.google.guava:listenablefuture:1.0"
const val GRADLE_INCAP_HELPER = "net.ltgt.gradle.incap:incap:0.2"
const val GRADLE_INCAP_HELPER_PROCESSOR = "net.ltgt.gradle.incap:incap-processor:0.2"
const val INTELLIJ_ANNOTATIONS = "com.intellij:annotations:12.0"
const val JAVAPOET = "com.squareup:javapoet:1.13.0"
const val JSQLPARSER = "com.github.jsqlparser:jsqlparser:3.1"
const val JSR250 = "javax.annotation:javax.annotation-api:1.2"
const val JUNIT = "junit:junit:4.12"
const val KOTLINPOET = "com.squareup:kotlinpoet:1.8.0"
const val KOTLIN_COMPILE_TESTING = "com.github.tschuchortdev:kotlin-compile-testing:1.4.0"
const val KOTLIN_COMPILE_TESTING_KSP = "com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.0"

/**
 * KSP is used both as a plugin and runtime dependency, hence its version is declared in the
 * build dependencies file.
 */
internal lateinit var kspVersion: String
val KSP_VERSION get() = kspVersion
const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.0"

const val KOTLIN_METADATA_JVM = "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0"

const val LEAKCANARY = "com.squareup.leakcanary:leakcanary-android:2.2"
const val LEAKCANARY_INSTRUMENTATION =
    "com.squareup.leakcanary:leakcanary-android-instrumentation:2.2"
const val MATERIAL = "com.google.android.material:material:1.2.1"
const val MOCKITO_CORE = "org.mockito:mockito-core:2.25.0"
const val MOCKITO_ANDROID = "org.mockito:mockito-android:2.25.0"
const val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
const val MULTIDEX = "androidx.multidex:multidex:2.0.1"
const val NULLAWAY = "com.uber.nullaway:nullaway:0.3.7"
const val PLAY_CORE = "com.google.android.play:core:1.9.1"
const val PLAY_SERVICES_BASEMENT = "com.google.android.gms:play-services-basement:17.0.0"
const val REACTIVE_STREAMS = "org.reactivestreams:reactive-streams:1.0.0"
const val RX_JAVA = "io.reactivex.rxjava2:rxjava:2.2.9"
const val RX_JAVA3 = "io.reactivex.rxjava3:rxjava:3.0.0"
val SKIKO_VERSION = System.getenv("SKIKO_VERSION") ?: "0.2.33"
val SKIKO = "org.jetbrains.skiko:skiko-jvm:$SKIKO_VERSION"
val SKIKO_LINUX_X64 = "org.jetbrains.skiko:skiko-jvm-runtime-linux-x64:$SKIKO_VERSION"
val SKIKO_MACOS_X64 = "org.jetbrains.skiko:skiko-jvm-runtime-macos-x64:$SKIKO_VERSION"
val SKIKO_MACOS_ARM64 = "org.jetbrains.skiko:skiko-jvm-runtime-macos-arm64:$SKIKO_VERSION"
val SKIKO_WINDOWS_X64 = "org.jetbrains.skiko:skiko-jvm-runtime-windows-x64:$SKIKO_VERSION"
val SKIKO_CURRENT_OS by lazy {
    val os = getOperatingSystem()
    val arch = System.getProperty("os.arch")
    when (os) {
        OperatingSystem.MAC -> when (arch) {
            "aarch64" -> SKIKO_MACOS_ARM64
            else -> SKIKO_MACOS_X64
        }
        OperatingSystem.WINDOWS -> SKIKO_WINDOWS_X64
        OperatingSystem.LINUX -> SKIKO_LINUX_X64
    }
}
const val TRUTH = "com.google.truth:truth:1.0.1"
const val VIEW_BINDING = "androidx.databinding:viewbinding:4.1.2"
const val XERIAL = "org.xerial:sqlite-jdbc:3.25.2"
const val XPP3 = "xpp3:xpp3:1.1.4c"
const val XMLPULL = "xmlpull:xmlpull:1.1.3.1"

const val RETROFIT = "com.squareup.retrofit2:retrofit:2.7.2"
const val OKHTTP_MOCKWEBSERVER = "com.squareup.okhttp3:mockwebserver:3.14.7"
const val SQLDELIGHT_ANDROID = "com.squareup.sqldelight:android-driver:1.3.0"
const val SQLDELIGHT_COROUTINES_EXT = "com.squareup.sqldelight:coroutines-extensions:1.3.0"

const val ROBOLECTRIC = "org.robolectric:robolectric:4.4-alpha-2"

const val PROTOBUF = "com.google.protobuf:protobuf-java:3.4.0"
const val PROTOBUF_COMPILER = "com.google.protobuf:protoc:3.10.0"
const val PROTOBUF_LITE = "com.google.protobuf:protobuf-javalite:3.10.0"

const val WIRE_RUNTIME = "com.squareup.wire:wire-runtime:3.6.0"

// The following versions change depending on whether we are in the main or ui project - the
// specific versions are configured in build_dependencies.gradle as they are needed during
// buildSrc configuration. They are then set here in AndroidXPlugin when configuring the root
// project.
internal lateinit var kotlinVersion: String

val KOTLIN_VERSION get() = kotlinVersion
val KOTLIN_STDLIB get() = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
val KOTLIN_STDLIB_COMMON get() = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
val KOTLIN_STDLIB_JDK8 get() = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
val KOTLIN_STDLIB_JS get() = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion"
val KOTLIN_TEST get() = "org.jetbrains.kotlin:kotlin-test:$kotlinVersion"
val KOTLIN_TEST_COMMON get() = "org.jetbrains.kotlin:kotlin-test-common:$kotlinVersion"
val KOTLIN_TEST_ANNOTATIONS_COMMON get() =
    "org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlinVersion"
val KOTLIN_TEST_JUNIT get() = "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion"
val KOTLIN_TEST_JS get() = "org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion"
val KOTLIN_REFLECT get() = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
val KOTLIN_COMPILER_EMBEDDABLE
    get() = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion"
val KOTLIN_COMPILER_DAEMON_EMBEDDABLE
    get() = "org.jetbrains.kotlin:kotlin-daemon-embeddable:$kotlinVersion"
val KOTLIN_ANNOTATION_PROCESSING_EMBEDDABLE
    get() = "org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:$kotlinVersion"

internal lateinit var kotlinCoroutinesVersion: String

val KOTLIN_COROUTINES_ANDROID
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_SWING
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-swing:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_CORE
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_GUAVA
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-guava:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_TEST
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_RX2
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_RX3
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-rx3:$kotlinCoroutinesVersion"

internal lateinit var agpVersion: String

const val AGP_STABLE = "com.android.tools.build:gradle:3.5.2"
val AGP_LATEST get() = "com.android.tools.build:gradle:$agpVersion"

internal lateinit var lintVersion: String
internal const val lintMinVersion = "26.3.0"

const val LINT_API_MIN = "com.android.tools.lint:lint-api:$lintMinVersion"
val LINT_API_LATEST get() = "com.android.tools.lint:lint-api:$lintVersion"
val LINT_CHECKS_LATEST get() = "com.android.tools.lint:lint-checks:$lintVersion"
val LINT_CORE get() = "com.android.tools.lint:lint:$lintVersion"
val LINT_TESTS get() = "com.android.tools.lint:lint-tests:$lintVersion"

const val MLKIT_GMS_BARCODE = "com.google.android.gms:play-services-mlkit-barcode-scanning:16.1.4"