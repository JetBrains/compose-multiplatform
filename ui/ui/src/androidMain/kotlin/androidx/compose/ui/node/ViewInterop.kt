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

package androidx.compose.ui.node

import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.compose.ui.viewinterop.InternalInteropApi
import kotlin.math.roundToInt

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ViewAdapter {
    val id: Int
    fun willInsert(view: View, parent: ViewGroup)
    fun didInsert(view: View, parent: ViewGroup)
    fun didUpdate(view: View, parent: ViewGroup)
}

/**
 * @suppress
 */
// TODO(b/150806128): We should decide if we want to make this public API or not. Right now it is needed
//  for convenient LayoutParams usage in compose with views.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T : ViewAdapter> View.getOrAddAdapter(id: Int, factory: () -> T): T {
    return getViewAdapter().get(id, factory)
}

/**
 * Intersects [Constraints] and [View] LayoutParams to obtain the suitable [View.MeasureSpec]
 * for measuring the [View].
 */
private fun obtainMeasureSpec(
    min: Int,
    max: Int,
    preferred: Int
): Int = when {
    preferred >= 0 || min == max -> {
        // Fixed size due to fixed size layout param or fixed constraints.
        View.MeasureSpec.makeMeasureSpec(
            preferred.coerceIn(min, max),
            View.MeasureSpec.EXACTLY
        )
    }
    preferred == ViewGroup.LayoutParams.WRAP_CONTENT && max != Constraints.Infinity -> {
        // Wrap content layout param with finite max constraint. If max constraint is infinite,
        // we will measure the child with UNSPECIFIED.
        View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST)
    }
    preferred == ViewGroup.LayoutParams.MATCH_PARENT && max != Constraints.Infinity -> {
        // Match parent layout param, so we force the child to fill the available space.
        View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.EXACTLY)
    }
    else -> {
        // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    }
}

/**
 * Builds a [LayoutNode] tree representation for an Android [View].
 * The component nodes will proxy the Compose core calls to the [View].
 */
@OptIn(InternalInteropApi::class)
internal fun AndroidViewHolder.toLayoutNode(): LayoutNode {
    // TODO(soboleva): add layout direction here?
    // TODO(popam): forward pointer input, accessibility, focus
    // Prepare layout node that proxies measure and layout passes to the View.
    val layoutNode = LayoutNode()

    val coreModifier = Modifier
        .pointerInteropFilter(this)
        .drawBehind {
            drawIntoCanvas { canvas ->
                (layoutNode.owner as? AndroidOwner)
                    ?.drawAndroidView(this@toLayoutNode, canvas.nativeCanvas)
            }
        }.onGloballyPositioned {
            // The global position of this LayoutNode can change with it being replaced. For these
            // cases, we need to inform the View.
            layoutAccordingTo(layoutNode)
        }
    layoutNode.modifier = modifier.then(coreModifier)
    onModifierChanged = { layoutNode.modifier = it.then(coreModifier) }

    layoutNode.density = density
    onDensityChanged = { layoutNode.density = it }

    var viewRemovedOnDetach: View? = null
    layoutNode.onAttach = { owner ->
        (owner as? AndroidOwner)?.addAndroidView(this, layoutNode)
        if (viewRemovedOnDetach != null) view = viewRemovedOnDetach
    }
    layoutNode.onDetach = { owner ->
        (owner as? AndroidOwner)?.removeAndroidView(this)
        viewRemovedOnDetach = view
        view = null
    }

    layoutNode.measureBlocks = object : LayoutNode.NoIntrinsicsMeasureBlocks(
        "Intrinsics not supported for Android views"
    ) {
        override fun measure(
            measureScope: MeasureScope,
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            if (constraints.minWidth != 0) {
                getChildAt(0).minimumWidth = constraints.minWidth
            }
            if (constraints.minHeight != 0) {
                getChildAt(0).minimumHeight = constraints.minHeight
            }
            // TODO (soboleva): native view should get LD value from Compose?

            // TODO(shepshapard): !! necessary?
            measure(
                obtainMeasureSpec(
                    constraints.minWidth,
                    constraints.maxWidth,
                    layoutParams!!.width
                ),
                obtainMeasureSpec(
                    constraints.minHeight,
                    constraints.maxHeight,
                    layoutParams!!.height
                )
            )
            return measureScope.layout(measuredWidth, measuredHeight) {
                layoutAccordingTo(layoutNode)
            }
        }
    }
    return layoutNode
}

private fun View.layoutAccordingTo(layoutNode: LayoutNode) {
    val position = layoutNode.coordinates.positionInRoot
    val x = position.x.roundToInt()
    val y = position.y.roundToInt()
    layout(x, y, x + measuredWidth, y + measuredHeight)
}

internal class MergedViewAdapter : ViewAdapter {
    override val id = 0
    val adapters = mutableListOf<ViewAdapter>()

    inline fun <T : ViewAdapter> get(id: Int, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val existing = adapters.fastFirstOrNull { it.id == id } as? T
        if (existing != null) return existing
        val next = factory()
        adapters.add(next)
        return next
    }

    override fun willInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.willInsert(view, parent) }
    }

    override fun didInsert(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didInsert(view, parent) }
    }

    override fun didUpdate(view: View, parent: ViewGroup) {
        adapters.fastForEach { it.didUpdate(view, parent) }
    }
}

/**
 * This function will take in a string and pass back a valid resource identifier for
 * View.setTag(...). We should eventually move this to a resource id that's actually generated via
 * AAPT but doing that in this project is proving to be complicated, so for now I'm just doing this
 * as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}

private val viewAdaptersKey = tagKey("ViewAdapter")

internal fun View.getViewAdapterIfExists(): MergedViewAdapter? {
    return getTag(viewAdaptersKey) as? MergedViewAdapter
}

internal fun View.getViewAdapter(): MergedViewAdapter {
    var adapter = getTag(viewAdaptersKey) as? MergedViewAdapter
    if (adapter == null) {
        adapter = MergedViewAdapter()
        setTag(viewAdaptersKey, adapter)
    }
    return adapter
}