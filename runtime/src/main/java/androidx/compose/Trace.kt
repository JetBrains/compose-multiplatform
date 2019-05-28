/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

import android.os.Trace

/**
 * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [sectionName])
 * and [Trace.endSection].
 */
inline fun <T> trace(sectionName: String, block: () -> T): T {
    Trace.beginSection(sectionName)
    try {
        return block()
    } finally {
        Trace.endSection()
    }
}