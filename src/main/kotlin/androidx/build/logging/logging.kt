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

package androidx.build.logging

import org.gradle.api.logging.LogLevel
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.slf4j.Marker

// TODO(b/132773278) move this functionality into Gradle itself?
open class LogUtils {
    companion object {
        fun turnWarningsIntoInfos(task: DefaultTask) {
            if (!task.logger.isEnabled(LogLevel.INFO)) {
                @Suppress("DEPRECATION")
                task.replaceLogger(ErrorLogger())
            }
        }
    }
}

open class ErrorLogger : org.gradle.api.logging.Logger {
    override fun isLifecycleEnabled() = false

    override fun trace(message: String) {}
    override fun trace(message: String, p: Any) {}
    override fun trace(message: String, p1: Any, p2: Any) {}
    override fun trace(message: String, vararg ps: Any) {}
    override fun trace(message: String, p: Throwable) {}
    override fun trace(message: Marker, p1: String) {}
    override fun trace(message: Marker, p1: String, p2: Any) {}
    override fun trace(message: Marker, p1: String, p2: Any, p3: Any) {}
    override fun trace(message: Marker, p1: String, vararg p2: Any) {}
    override fun trace(message: Marker, p1: String, p2: Throwable) {}

    override fun debug(message: String) {}
    override fun debug(message: String, p: Any) {}
    override fun debug(message: String, p1: Any, p2: Any) {}
    override fun debug(message: String, vararg ps: Any) {}
    override fun debug(message: String, p: Throwable) {}
    override fun debug(message: Marker, p1: String) {}
    override fun debug(message: Marker, p1: String, p2: Any) {}
    override fun debug(message: Marker, p1: String, p2: Any, p3: Any) {}
    override fun debug(message: Marker, p1: String, vararg p2: Any) {}
    override fun debug(message: Marker, p1: String, p2: Throwable) {}

    override fun quiet(message: String) {}
    override fun quiet(message: String, vararg ps: Any) {}
    override fun quiet(message: String, p: Throwable) {}

    override fun warn(message: String) {}
    override fun warn(message: String, p: Any) {}
    override fun warn(message: String, p1: Any, p2: Any) {}
    override fun warn(message: String, vararg ps: Any) {}
    override fun warn(message: String, p: Throwable) {}
    override fun warn(message: Marker, p1: String) {}
    override fun warn(message: Marker, p1: String, p2: Any) {}
    override fun warn(message: Marker, p1: String, p2: Any, p3: Any) {}
    override fun warn(message: Marker, p1: String, vararg p2: Any) {}
    override fun warn(message: Marker, p1: String, p2: Throwable) {}

    override fun info(message: String) {}
    override fun info(message: String, p: Any) {}
    override fun info(message: String, p1: Any, p2: Any) {}
    override fun info(message: String, vararg ps: Any) {}
    override fun info(message: String, p: Throwable) {}
    override fun info(message: Marker, p1: String) {}
    override fun info(message: Marker, p1: String, p2: Any) {}
    override fun info(message: Marker, p1: String, p2: Any, p3: Any) {}
    override fun info(message: Marker, p1: String, vararg p2: Any) {}
    override fun info(message: Marker, p1: String, p2: Throwable) {}

    override fun lifecycle(message: String) {}
    override fun lifecycle(message: String, vararg ps: Any) {}
    override fun lifecycle(message: String, p: Throwable) {}

    override fun error(message: String) { print(message) }
    override fun error(message: String, p: Any) { print("$message $p") }
    override fun error(message: String, p1: Any, p2: Any) { print("$message $p1 $p2") }
    override fun error(message: String, vararg ps: Any) { print("$message $ps") }
    override fun error(message: String, p: Throwable) { print("$message $p}") }
    override fun error(message: Marker, p1: String) { print("$message $p1}") }
    override fun error(message: Marker, p1: String, p2: Any) { print("$message $p1 $p2") }
    override fun error(message: Marker, p1: String, p2: Any, p3: Any) {
        print("$message $p1 $p2 $p3")
    }
    override fun error(message: Marker, p1: String, vararg p2: Any) { print("$message $p1 $p2") }
    override fun error(message: Marker, p1: String, p2: Throwable) { print("$message $p1 $p2") }

    override fun log(level: LogLevel, message: String) {}
    override fun log(level: LogLevel, message: String, vararg p: Any) {}
    override fun log(level: LogLevel, message: String, p1: Throwable) {}

    override fun isTraceEnabled() = false
    override fun isTraceEnabled(marker: Marker) = false
    override fun isDebugEnabled() = false
    override fun isDebugEnabled(marker: Marker) = false
    override fun isQuietEnabled() = false
    override fun isInfoEnabled() = false
    override fun isInfoEnabled(marker: Marker) = false
    override fun isWarnEnabled() = false
    override fun isWarnEnabled(marker: Marker) = false
    override fun isErrorEnabled() = false
    override fun isErrorEnabled(marker: Marker) = false
    override fun isEnabled(level: LogLevel) = false

    override fun getName() = "ErrorLogger"
}
