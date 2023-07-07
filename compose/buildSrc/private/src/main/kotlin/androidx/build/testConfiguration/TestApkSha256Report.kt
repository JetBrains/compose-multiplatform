/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.testConfiguration

import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import java.io.File

@Suppress("UnstableApiUsage") // guava Hashing is marked as @Beta
internal fun sha256(file: File): String {
    val hasher = Hashing.sha256().newHasher()
    file.inputStream().buffered().use {
        while (it.available() > 0) {
            hasher.putBytes(it.readNBytes(1024))
        }
    }
    return BaseEncoding.base16().lowerCase().encode(
        hasher.hash().asBytes()
    )
}