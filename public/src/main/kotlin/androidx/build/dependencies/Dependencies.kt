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

lateinit var guavaVersion: String
val GUAVA_VERSION get() = guavaVersion

lateinit var kspVersion: String
val KSP_VERSION get() = kspVersion

lateinit var kotlinVersion: String
val KOTLIN_VERSION get() = kotlinVersion
lateinit var kotlinNativeVersion: String
val KOTLIN_NATIVE_VERSION get() = kotlinNativeVersion
val KOTLIN_STDLIB get() = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"

lateinit var agpVersion: String
val AGP_LATEST get() = "com.android.tools.build:gradle:$agpVersion"
