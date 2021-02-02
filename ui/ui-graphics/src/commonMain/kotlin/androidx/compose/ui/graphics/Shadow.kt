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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.util.lerp

/**
 * A single shadow.
 */
@Immutable
class Shadow(
    @Stable
    val color: Color = Color(0xFF000000),
    @Stable
    val offset: Offset = Offset.Zero,
    @Stable
    val blurRadius: Float = 0.0f
) {
    companion object {
        /**
         * Constant for no shadow.
         */
        @Stable
        val None = Shadow()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shadow) return false

        if (color != other.color) return false
        if (offset != other.offset) return false
        if (blurRadius != other.blurRadius) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + blurRadius.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shadow(color=$color, offset=$offset, blurRadius=$blurRadius)"
    }

    fun copy(
        color: Color = this.color,
        offset: Offset = this.offset,
        blurRadius: Float = this.blurRadius
    ): Shadow {
        return Shadow(
            color = color,
            offset = offset,
            blurRadius = blurRadius
        )
    }
}

/**
 * Linearly interpolate two [Shadow]s.
 */
@Stable
fun lerp(start: Shadow, stop: Shadow, fraction: Float): Shadow {
    return Shadow(
        lerp(start.color, stop.color, fraction),
        lerp(start.offset, stop.offset, fraction),
        lerp(start.blurRadius, stop.blurRadius, fraction)
    )
}