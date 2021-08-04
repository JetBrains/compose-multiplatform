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

package androidx.build.dependencyTracker

import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLogger
import org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext
import org.gradle.internal.time.Clock

/**
 * Gradle logger that logs to a string.
 */
class ToStringLogger(
    private val stringBuilder: StringBuilder = StringBuilder()
) : OutputEventListenerBackedLogger(
    "my_logger",
    OutputEventListenerBackedLoggerContext(
        Clock {
            System.currentTimeMillis()
        }
    ).also {
        it.level = LogLevel.DEBUG
        it.setOutputEventListener {
            stringBuilder.append(it.toString() + "\n")
        }
    },
    Clock {
        System.currentTimeMillis()
    }
) {
    /**
     * Returns the current log.
     */
    fun buildString() = stringBuilder.toString()
}
