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

package androidx.compose.ui.text

/**
 * When to replace emoji with support emoji using androidx.emoji2.
 *
 * This is only available on Android.
 */
@kotlin.jvm.JvmInline
value class EmojiSupportMatch private constructor(private val value: Int) {

    override fun toString(): String {
        return when (value) {
            Default.value -> "EmojiSupportMatch.Default"
            None.value -> "EmojiSupportMatch.None"
            else -> "Invalid(value=$value)"
        }
    }

    companion object {

        /**
         * Default support strategy defers to EmojiCompat.get()
         */
        val Default = EmojiSupportMatch(0)

        /**
         * Do not use support emoji for this paragraph.
         */
        val None = EmojiSupportMatch(1)
    }
}