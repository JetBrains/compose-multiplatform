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

const val ANDROIDX_TEST_VERSION = "1.4.0-beta01"
const val ANDROIDX_TEST_CORE = "androidx.test:core:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_EXT_JUNIT = "androidx.test.ext:junit:1.1.3-beta01"
const val ANDROIDX_TEST_MONITOR = "androidx.test:monitor:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_RULES = "androidx.test:rules:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_RUNNER = "androidx.test:runner:$ANDROIDX_TEST_VERSION"
const val ANDROIDX_TEST_UIAUTOMATOR = "androidx.test.uiautomator:uiautomator:2.2.0"

const val AUTO_COMMON = "com.google.auto:auto-common:0.11"
const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:2.0.1@aar"
const val DAGGER = "com.google.dagger:dagger:2.35"
const val DEXMAKER_MOCKITO = "com.linkedin.dexmaker:dexmaker-mockito:2.25.0"
const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:3.3.0"
const val ESPRESSO_INTENTS = "androidx.test.espresso:espresso-intents:3.3.0"
const val GOOGLE_COMPILE_TESTING = "com.google.testing.compile:compile-testing:0.18"
const val GUAVA_VERSION = "29.0-jre"
const val GUAVA = "com.google.guava:guava:$GUAVA_VERSION"
const val GUAVA_ANDROID_VERSION = "29.0-android"
const val GUAVA_ANDROID = "com.google.guava:guava:$GUAVA_ANDROID_VERSION"
const val JAVAPOET = "com.squareup:javapoet:1.13.0"
const val JSR250 = "javax.annotation:javax.annotation-api:1.2"
const val JUNIT = "junit:junit:4.12"

/**
 * KSP is used both as a plugin and runtime dependency, hence its version is declared in the
 * build dependencies file.
 */
internal lateinit var kspVersion: String
val KSP_VERSION get() = kspVersion

const val KOTLIN_METADATA_JVM = "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.3.0"

const val MATERIAL = "com.google.android.material:material:1.2.1"
const val MOCKITO_CORE = "org.mockito:mockito-core:2.25.0"
const val MOCKITO_ANDROID = "org.mockito:mockito-android:2.25.0"
const val MULTIDEX = "androidx.multidex:multidex:2.0.1"
const val TRUTH = "com.google.truth:truth:1.0.1"

const val PROTOBUF = "com.google.protobuf:protobuf-java:3.4.0"
const val PROTOBUF_COMPILER = "com.google.protobuf:protoc:3.10.0"
const val PROTOBUF_LITE = "com.google.protobuf:protobuf-javalite:3.10.0"

// The following versions change depending on whether we are in the main or ui project - the
// specific versions are configured in build_dependencies.gradle as they are needed during
// buildSrc configuration. They are then set here in AndroidXPlugin when configuring the root
// project.
internal lateinit var kotlinVersion: String

val KOTLIN_VERSION get() = kotlinVersion
val KOTLIN_STDLIB get() = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
val KOTLIN_STDLIB_COMMON get() = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
val KOTLIN_STDLIB_JDK8 get() = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
val KOTLIN_TEST get() = "org.jetbrains.kotlin:kotlin-test:$kotlinVersion"
val KOTLIN_TEST_COMMON get() = "org.jetbrains.kotlin:kotlin-test-common:$kotlinVersion"
val KOTLIN_REFLECT get() = "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"

internal lateinit var kotlinCoroutinesVersion: String

val KOTLIN_COROUTINES_ANDROID
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_CORE
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion"
val KOTLIN_COROUTINES_TEST
    get() = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion"

internal lateinit var agpVersion: String

val AGP_LATEST get() = "com.android.tools.build:gradle:$agpVersion"

internal lateinit var lintVersion: String
internal const val lintMinVersion = "26.3.0"

const val LINT_API_MIN = "com.android.tools.lint:lint-api:$lintMinVersion"
val LINT_API_LATEST get() = "com.android.tools.lint:lint-api:$lintVersion"
val LINT_CORE get() = "com.android.tools.lint:lint:$lintVersion"
val LINT_TESTS get() = "com.android.tools.lint:lint-tests:$lintVersion"
