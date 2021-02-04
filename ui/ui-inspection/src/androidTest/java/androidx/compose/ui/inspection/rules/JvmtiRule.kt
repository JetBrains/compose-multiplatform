/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection.rules

import android.os.Debug
import org.junit.rules.ExternalResource
import java.io.IOException

class JvmtiRule : ExternalResource() {
    companion object {
        init {
            try {
                Debug.attachJvmtiAgent("nonexistent.so", null, null)
                throw AssertionError("attachJvmtiAgent must fail")
            } catch (e: IOException) {
                // expected: "nonexistent.so" doesn't exist, however attachJvmtiAgent call is enough
                // to make art to load JVMTI plugin.
            }
        }
        fun ensureInitialised() {
            // Calling this makes sure init {} block is triggered
        }
    }

    override fun before() {
        ensureInitialised()
    }
}