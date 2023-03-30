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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi

/**
 * Implementation of possible TextMotion configurations on Android.
 */
@ExperimentalTextApi
@Immutable
actual class TextMotion internal constructor(
    internal val linearity: Linearity,
    internal val subpixelTextPositioning: Boolean
) {
    actual companion object {
        actual val Static: TextMotion = TextMotion(Linearity.FontHinting, false)
        actual val Animated: TextMotion = TextMotion(Linearity.Linear, true)
    }

    internal fun copy(
        linearity: Linearity = this.linearity,
        subpixelTextPositioning: Boolean = this.subpixelTextPositioning
    ): TextMotion = TextMotion(
        linearity = linearity,
        subpixelTextPositioning = subpixelTextPositioning
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextMotion) return false

        if (linearity != other.linearity) return false
        if (subpixelTextPositioning != other.subpixelTextPositioning) return false

        return true
    }

    override fun hashCode(): Int {
        var result = linearity.hashCode()
        result = 31 * result + subpixelTextPositioning.hashCode()
        return result
    }

    override fun toString(): String {
        return when (this) {
            Static -> "TextMotion.Static"
            Animated -> "TextMotion.Animated"
            else -> "Invalid"
        }
    }

    /**
     * Defines the possible valid configurations for text linearity on Android platform. Both font
     * hinting and Linear text cannot be enabled at the same time. Disabling both, [Linearity.None],
     * may render the same output as [Linearity.Linear] on many OEM and API levels.
     */
    @JvmInline
    internal value class Linearity private constructor(private val value: Int) {
        companion object {
            /**
             * Equal to applying [android.graphics.Paint.LINEAR_TEXT_FLAG] and turning hinting off.
             */
            val Linear = Linearity(1)

            /**
             * Equal to removing [android.graphics.Paint.LINEAR_TEXT_FLAG] and turning hinting on.
             */
            val FontHinting = Linearity(2)

            /**
             * Equal to removing [android.graphics.Paint.LINEAR_TEXT_FLAG] and turning hinting off.
             */
            val None = Linearity(3)
        }

        override fun toString(): String = when (this) {
            Linear -> "Linearity.Linear"
            FontHinting -> "Linearity.FontHinting"
            None -> "Linearity.None"
            else -> "Invalid"
        }
    }
}