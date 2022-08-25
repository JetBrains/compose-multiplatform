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

package androidx.compose.foundation.lazy

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal class LazyItemScopeImpl : LazyItemScope {

    private var maxWidthState: MutableState<Int> = mutableStateOf(Int.MAX_VALUE)
    private var maxHeightState: MutableState<Int> = mutableStateOf(Int.MAX_VALUE)

    fun setMaxSize(width: Int, height: Int) {
        maxWidthState.value = width
        maxHeightState.value = height
    }

    override fun Modifier.fillParentMaxSize(fraction: Float) = then(
        ParentSizeModifier(
            widthState = maxWidthState,
            heightState = maxHeightState,
            fraction = fraction,
            inspectorInfo = debugInspectorInfo {
                name = "fillParentMaxSize"
                value = fraction
            }
        )
    )

    override fun Modifier.fillParentMaxWidth(fraction: Float) = then(
        ParentSizeModifier(
            widthState = maxWidthState,
            fraction = fraction,
            inspectorInfo = debugInspectorInfo {
                name = "fillParentMaxWidth"
                value = fraction
            }
        )
    )

    override fun Modifier.fillParentMaxHeight(fraction: Float) = then(
        ParentSizeModifier(
            heightState = maxHeightState,
            fraction = fraction,
            inspectorInfo = debugInspectorInfo {
                name = "fillParentMaxHeight"
                value = fraction
            }
        )
    )

    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>) =
        this.then(AnimateItemPlacementModifier(animationSpec, debugInspectorInfo {
            name = "animateItemPlacement"
            value = animationSpec
        }))
}

private class ParentSizeModifier(
    val fraction: Float,
    inspectorInfo: InspectorInfo.() -> Unit,
    val widthState: State<Int>? = null,
    val heightState: State<Int>? = null,
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val width = if (widthState != null) {
            (widthState.value * fraction).roundToInt()
        } else {
            Int.MAX_VALUE
        }
        val height = if (heightState != null) {
            (heightState.value * fraction).roundToInt()
        } else {
            Int.MAX_VALUE
        }
        val childConstraints = Constraints(
            minWidth = if (width != Int.MAX_VALUE) width else constraints.minWidth,
            minHeight = if (height != Int.MAX_VALUE) height else constraints.minHeight,
            maxWidth = if (width != Int.MAX_VALUE) width else constraints.maxWidth,
            maxHeight = if (height != Int.MAX_VALUE) height else constraints.maxHeight,
        )
        val placeable = measurable.measure(childConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParentSizeModifier) return false
        return widthState == other.widthState &&
            heightState == other.heightState &&
            fraction == other.fraction
    }

    override fun hashCode(): Int {
        var result = widthState?.hashCode() ?: 0
        result = 31 * result + (heightState?.hashCode() ?: 0)
        result = 31 * result + fraction.hashCode()
        return result
    }
}

private class AnimateItemPlacementModifier(
    val animationSpec: FiniteAnimationSpec<IntOffset>,
    inspectorInfo: InspectorInfo.() -> Unit,
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {
    override fun Density.modifyParentData(parentData: Any?): Any = animationSpec

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimateItemPlacementModifier) return false
        return animationSpec != other.animationSpec
    }

    override fun hashCode(): Int {
        return animationSpec.hashCode()
    }
}
