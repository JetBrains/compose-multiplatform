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

package androidx.compose.material3

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.tokens.Shape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Material surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * Components are grouped into shape categories based on their size. These categories provide a
 * way to change multiple component values at once, by changing the category’s values.
 * Shape categories include:
 * - Small components
 * - Medium components
 * - Large components
 *
 * See [Material shape specification](https://material.io/design/shape/applying-shape-to-ui.html)
 */
@Immutable
class Shapes(
    // TODO(connieshi): add examples.
    /**
     * Shape used by small components.
     */
    val small: CornerBasedShape = Shape.Small,
    // TODO(connieshi): add examples.
    /**
     * Shape used by medium components.
     */
    val medium: CornerBasedShape = Shape.Medium,
    // TODO(connieshi): add examples.
    /**
     * Shape used by large components.
     */
    val large: CornerBasedShape = Shape.Large
) {

    /**
     * Returns a copy of this Shapes, optionally overriding some of the values.
     */
    fun copy(
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large
    ): Shapes = Shapes(
        small = small,
        medium = medium,
        large = large
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false

        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false

        return true
    }

    override fun hashCode(): Int {
        var result = small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shapes(small=$small, medium=$medium, large=$large)"
    }
}

/**
 * CompositionLocal used to specify the default shapes for the surfaces.
 */
internal val LocalShapes = staticCompositionLocalOf { Shapes() }
