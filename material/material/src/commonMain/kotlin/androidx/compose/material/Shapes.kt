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

package androidx.compose.material

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.unit.dp

/**
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
data class Shapes(
    /**
     * Shape used by small components like [Button] or [Snackbar]. Components like
     * [FloatingActionButton], [ExtendedFloatingActionButton] use this shape, but override
     * the corner size to be 50%. [TextField] uses this shape with overriding the bottom corners
     * to zero.
     */
    val small: CornerBasedShape = RoundedCornerShape(4.dp),
    /**
     * Shape used by medium components like [Card] or [AlertDialog].
     */
    val medium: CornerBasedShape = RoundedCornerShape(4.dp),
    /**
     * Shape used by large components like [ModalDrawerLayout] or [BottomDrawerLayout].
     */
    val large: CornerBasedShape = RoundedCornerShape(0.dp)
)

/**
 * Ambient used to specify the default shapes for the surfaces.
 */
internal val AmbientShapes = staticAmbientOf { Shapes() }
