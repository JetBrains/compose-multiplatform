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
package androidx.compose.ui.inspection

data class LambdaLocation(val fileName: String, val startLine: Int, val endLine: Int) {
    companion object {
        init {
            System.loadLibrary("compose_inspection_jni")
        }

        fun resolve(o: Any): LambdaLocation? {
            return resolve(o::class.java)
        }

        @JvmStatic
        private external fun resolve(clazz: Class<*>): LambdaLocation?
    }
}
