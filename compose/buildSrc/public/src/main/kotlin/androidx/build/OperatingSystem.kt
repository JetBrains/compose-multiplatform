/*
 * Copyright 2020 The Android Open Source Project
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

import org.gradle.api.GradleException
import java.util.Locale

enum class OperatingSystem {
    LINUX,
    WINDOWS,
    MAC
}

fun getOperatingSystem(): OperatingSystem {
    val os = System.getProperty("os.name").lowercase(Locale.US)
    return when {
        os.contains("mac os x") -> OperatingSystem.MAC
        os.contains("darwin") -> OperatingSystem.MAC
        os.contains("osx") -> OperatingSystem.MAC
        os.startsWith("win") -> OperatingSystem.WINDOWS
        os.startsWith("linux") -> OperatingSystem.LINUX
        else -> throw GradleException("Unsupported operating system $os")
    }
}