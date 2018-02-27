/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import org.gradle.api.Project

/**
 * Extension for [SupportAndroidTestAppPlugin].
 */
open class SupportAndroidTestAppExtension(val project: Project) {
    /**
     * If unset minSdkVersion will be [DEFAULT_MIN_SDK_VERSION].
     */
    var minSdkVersion: Int = DEFAULT_MIN_SDK_VERSION

    /**
     * Modifies the java compile tasks to run with error prone.
     * This can be useful for code generators to assert that the generated code won't cause trouble
     * for the developers.
     * <p>
     * Enabling this modifies all of the Javac tasks in the project to run with error prone.
     */
    var enableErrorProne: Boolean = false
}