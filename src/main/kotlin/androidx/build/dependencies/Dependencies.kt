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

const val ANDROIDX_TEST_CORE = "androidx.test:core:1.1.0"
const val ANDROIDX_TEST_EXT_JUNIT = "androidx.test.ext:junit:1.1.0"
const val ANDROIDX_TEST_EXT_KTX = "androidx.test.ext:junit-ktx:1.1.0"
const val ANDROIDX_TEST_RULES = "androidx.test:rules:1.1.0"
const val ANDROIDX_TEST_RUNNER = "androidx.test:runner:1.1.1"
const val ANDROIDX_TEST_UIAUTOMATOR = "androidx.test.uiautomator:uiautomator:2.2.0"
const val AUTO_COMMON = "com.google.auto:auto-common:0.10"
const val AUTO_VALUE = "com.google.auto.value:auto-value:1.6.3"
const val AUTO_VALUE_ANNOTATIONS = "com.google.auto.value:auto-value-annotations:1.6.3"
const val AUTO_VALUE_PARCEL = "com.ryanharter.auto.value:auto-value-parcel:0.2.6"
const val ANTLR = "org.antlr:antlr4:4.7.1"
const val APACHE_COMMONS_CODEC = "commons-codec:commons-codec:1.10"
const val CHECKER_FRAMEWORK = "org.checkerframework:checker-qual:2.5.3"
const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:1.1.0@aar"
const val DEXMAKER_MOCKITO = "com.linkedin.dexmaker:dexmaker-mockito:2.19.0"
const val ESPRESSO_CONTRIB = "androidx.test.espresso:espresso-contrib:3.1.0"
const val ESPRESSO_CORE = "androidx.test.espresso:espresso-core:3.1.0"
const val ESPRESSO_IDLING_RESOURCE = "androidx.test.espresso:espresso-idling-resource:3.1.0"
const val FINDBUGS = "com.google.code.findbugs:jsr305:3.0.2"
const val GCM_NETWORK_MANAGER = "com.google.android.gms:play-services-gcm:16.0.0-jetified"
const val GOOGLE_COMPILE_TESTING = "com.google.testing.compile:compile-testing:0.11"
const val GSON = "com.google.code.gson:gson:2.8.0"
const val GUAVA = "com.google.guava:guava:27.0.1-jre"
const val GUAVA_ANDROID = "com.google.guava:guava:27.0.1-android"
const val GUAVA_LISTENABLE_FUTURE = "com.google.guava:listenablefuture:1.0"
const val INTELLIJ_ANNOTATIONS = "com.intellij:annotations:12.0"
const val JAVAPOET = "com.squareup:javapoet:1.8.0"
const val JSR250 = "javax.annotation:javax.annotation-api:1.2"
const val JUNIT = "junit:junit:4.12"
const val KOTLINPOET = "com.squareup:kotlinpoet:1.1.0"

private const val KOTLIN_VERSION = "1.3.40"
const val KOTLIN_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
const val KOTLIN_TEST_COMMON = "org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION"
const val COMPOSE_VERSION = "1.3.30-compose-20190503"
const val KOTLIN_COMPOSE_STDLIB = "org.jetbrains.kotlin:kotlin-stdlib:$COMPOSE_VERSION"
const val KOTLIN_COMPOSE_REFLECT = "org.jetbrains.kotlin:kotlin-reflect:$COMPOSE_VERSION"

const val KOTLIN_METADATA = "me.eugeniomarletti.kotlin.metadata:kotlin-metadata:1.4.0"
const val KOTLIN_METADATA_JVM = "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.0.5"
const val KOTLIN_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.1.1"
const val KOTLIN_COMPOSE_COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.1.1"
const val KOTLIN_COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1"
const val KOTLIN_COROUTINES_TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.1.1"
const val LEAKCANARY_INSTRUMENTATION =
        "com.squareup.leakcanary:leakcanary-android-instrumentation:1.6.2"
const val LINT_API_MIN = "com.android.tools.lint:lint-api:26.3.0"
const val LINT_API_LATEST = "com.android.tools.lint:lint-api:26.5.0-beta04"
const val LINT_CORE = "com.android.tools.lint:lint:26.5.0-beta04"
const val LINT_TESTS = "com.android.tools.lint:lint-tests:26.5.0-beta04"
const val MATERIAL = "com.google.android.material:material:1.0.0"
const val MOCKITO_CORE = "org.mockito:mockito-core:2.19.0"
const val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0"
const val MULTIDEX = "androidx.multidex:multidex:2.0.0"
const val NULLAWAY = "com.uber.nullaway:nullaway:0.3.7"
const val OKHTTP_MOCKWEBSERVER = "com.squareup.okhttp3:mockwebserver:3.11.0"
const val REACTIVE_STREAMS = "org.reactivestreams:reactive-streams:1.0.0"
const val RX_JAVA = "io.reactivex.rxjava2:rxjava:2.2.9"
const val TRUTH = "com.google.truth:truth:0.44"
const val XERIAL = "org.xerial:sqlite-jdbc:3.25.2"
const val XPP3 = "xpp3:xpp3:1.1.4c"
const val XMLPULL = "xmlpull:xmlpull:1.1.3.1"

private const val SUPPORT_VERSION = "1.0.0"
const val SUPPORT_ANNOTATIONS = "androidx.annotation:annotation:$SUPPORT_VERSION"
const val SUPPORT_APPCOMPAT = "androidx.appcompat:appcompat:$SUPPORT_VERSION"
const val SUPPORT_CORE_UTILS = "androidx.legacy:legacy-support-core-utils:$SUPPORT_VERSION"
const val SUPPORT_FRAGMENTS = "androidx.fragment:fragment:$SUPPORT_VERSION"
const val SUPPORT_RECYCLERVIEW = "androidx.recyclerview:recyclerview:$SUPPORT_VERSION"

const val ANDROIDX_ANNOTATION = "androidx.annotation:annotation:1.1.0"
const val ANDROIDX_COLLECTION = "androidx.collection:collection:$SUPPORT_VERSION"
const val ANDROIDX_CORE = "androidx.core:core:1.0.0"
const val ANDROIDX_RECYCLERVIEW = "androidx.recyclerview:recyclerview:1.0.0"
const val ANDROIDX_SQLITE = "androidx.sqlite:sqlite:2.0.1"
const val ANDROIDX_SQLITE_FRAMEWORK = "androidx.sqlite:sqlite-framework:2.0.1"

// Arch libraries
const val ARCH_LIFECYCLE_RUNTIME = "androidx.lifecycle:lifecycle-runtime:2.0.0"
const val ARCH_LIFECYCLE_COMMON = "androidx.lifecycle:lifecycle-common:2.0.0@jar"
const val ARCH_LIFECYCLE_LIVEDATA_CORE = "androidx.lifecycle:lifecycle-livedata-core:2.0.0"
const val ARCH_LIFECYCLE_LIVEDATA = "androidx.lifecycle:lifecycle-livedata:2.0.0"
const val ARCH_LIFECYCLE_SERVICE = "androidx.lifecycle:lifecycle-service:2.0.0"
const val ARCH_LIFECYCLE_VIEWMODEL = "androidx.lifecycle:lifecycle-viewmodel:2.0.0"
const val ARCH_LIFECYCLE_VIEWMODEL_KTX = "androidx.lifecycle:lifecycle-viewmodel-ktx:2.0.0"
const val ARCH_LIFECYCLE_EXTENSIONS = "androidx.lifecycle:lifecycle-extensions:2.0.0"
const val ARCH_CORE_COMMON = "androidx.arch.core:core-common:2.0.1@jar"
const val ARCH_CORE_RUNTIME = "androidx.arch.core:core-runtime:2.0.1"
const val ARCH_CORE_TESTING = "androidx.arch.core:core-testing:2.0.1"

const val SAFE_ARGS_ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:3.4.0"
const val SAFE_ARGS_KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.20"
const val SAFE_ARGS_NAVIGATION_COMMON = "androidx.navigation:navigation-common:2.0.0-rc02"

const val ARCH_PAGING_COMMON = "androidx.paging:paging-common:2.0.0"
const val ARCH_PAGING_RUNTIME = "androidx.paging:paging-runtime:2.0.0"
const val ARCH_ROOM_RUNTIME = "androidx.room:room-runtime:2.0.0"
const val ARCH_ROOM_COMPILER = "androidx.room:room-compiler:2.0.0"
const val ARCH_ROOM_RXJAVA = "androidx.room:room-rxjava2:2.0.0"
const val ARCH_ROOM_TESTING = "androidx.room:room-testing:2.0.0"

const val WORK_ARCH_CORE_RUNTIME = "androidx.arch.core:core-runtime:2.0.0"
const val WORK_ARCH_CORE_TESTING = "androidx.arch.core:core-testing:2.0.0"
const val WORK_ARCH_ROOM_RUNTIME = "androidx.room:room-runtime:2.1.0"
const val WORK_ARCH_ROOM_COMPILER = "androidx.room:room-compiler:2.1.0"
const val WORK_ARCH_ROOM_TESTING = "androidx.room:room-testing:2.1.0"

const val ROBOLECTRIC = "org.robolectric:robolectric:4.1"

const val PROTOBUF = "com.google.protobuf:protobuf-java:3.4.0"
