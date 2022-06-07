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

package androidx.compose.runtime

@Suppress("unused")
internal object ComposeVersion {
    /**
     * This version number is used by the compose compiler in order to verify that the compiler
     * and the runtime are compatible with one another.
     *
     * Every release should increase this number to a multiple of 100, which provides for the
     * opportunity to use the last two digits for releases made out-of-band.
     *
     * IMPORTANT: Whenever updating this value, please make sure to also update `versionTable` and
     * `minimumRuntimeVersionInt` in `VersionChecker.kt` of the compiler.
     */
    const val version: Int = 7100
}
