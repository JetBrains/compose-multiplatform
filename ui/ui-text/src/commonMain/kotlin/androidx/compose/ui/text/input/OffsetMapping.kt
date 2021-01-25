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

package androidx.compose.ui.text.input

/**
 * Provides bidirectional offset mapping between original and transformed text.
 */
interface OffsetMapping {
    /**
     * Convert offset in original text into the offset in transformed text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the original text, the cursor in the transformed text must advance or stay there.
     *
     * @param offset offset in original text.
     * @return offset in transformed text
     *
     * @see VisualTransformation
     */
    fun originalToTransformed(offset: Int): Int

    /**
     * Convert offset in transformed text into the offset in original text.
     *
     * This function must be a monotonically non-decreasing function. In other words, if a cursor
     * advances in the transformed text, the cusrsor in the original text must advance or stay
     * there.
     *
     * @param offset offset in transformed text
     * @return offset in original text
     *
     * @see VisualTransformation
     */
    fun transformedToOriginal(offset: Int): Int

    companion object {
        /**
         * The offset map used for identity mapping.
         */
        val Identity = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }
    }
}
