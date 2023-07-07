/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent

/**
 * Key combiner which buffers dead keys and combines them with subsequent keys as necessary.
 *
 * It is NOT thread safe.
 */
internal expect class DeadKeyCombiner() {

    /**
     * @param event the key event received by the combiner
     * @return a unicode code point to emit in response to the event,
     * or null if no code point should be emitted
     */
    fun consume(event: KeyEvent): Int?
}
