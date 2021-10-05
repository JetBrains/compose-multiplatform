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

package androidx.compose.ui.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The public information about the layouts used internally as nodes in the Compose UI hierarchy.
 */
interface LayoutInfo {

    /**
     * This returns a new List of [Modifier]s and the coordinates and any extra information
     * that may be useful. This is used for tooling to retrieve layout modifier and layer
     * information.
     */
    fun getModifierInfo(): List<ModifierInfo>

    /**
     * The measured width of this layout and all of its modifiers.
     */
    val width: Int

    /**
     * The measured height of this layout and all of its modifiers.
     */
    val height: Int

    /**
     * Coordinates of just the contents of the layout, after being affected by all modifiers.
     */
    val coordinates: LayoutCoordinates

    /**
     * Whether or not this layout and all of its parents have been placed in the hierarchy.
     */
    val isPlaced: Boolean

    /**
     * Parent of this layout.
     */
    val parentInfo: LayoutInfo?

    /**
     * The density in use for this layout.
     */
    val density: Density

    /**
     * The layout direction in use for this layout.
     */
    val layoutDirection: LayoutDirection

    /**
     * The [ViewConfiguration] in use for this layout.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Returns true if this layout is currently a part of the layout tree.
     */
    val isAttached: Boolean
}

/**
 * Used by tooling to examine the modifiers on a [LayoutInfo].
 */
class ModifierInfo(
    val modifier: Modifier,
    val coordinates: LayoutCoordinates,
    val extra: Any? = null
)

/**
 * The info about the graphics layers used by tooling.
 */
interface GraphicLayerInfo {
    /**
     * The ID of the layer. This is used by tooling to match a layer to the associated
     * LayoutNode.
     */
    val layerId: Long

    /**
     * The uniqueDrawingId of the owner view of this graphics layer. This is used by
     * tooling to match a layer to the associated owner AndroidComposeView.
     */
    val ownerViewId: Long
        get() = 0
}
