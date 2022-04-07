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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.graphics.Rect
import android.graphics.Region
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.composeToViewOffset
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * A base class used to host a [View] inside Compose.
 * This API is not designed to be used directly, but rather using the [AndroidView] and
 * `AndroidViewBinding` APIs, which are built on top of [AndroidViewHolder].
 */
@OptIn(ExperimentalComposeUiApi::class)
internal abstract class AndroidViewHolder(
    context: Context,
    parentContext: CompositionContext?,
    private val dispatcher: NestedScrollDispatcher
) : ViewGroup(context), NestedScrollingParent3 {

    init {
        // Any [Abstract]ComposeViews that are descendants of this view will host
        // subcompositions of the host composition.
        // UiApplier doesn't supply this, only AndroidView.
        parentContext?.let {
            compositionContext = it
        }
        // We save state ourselves, depending on composition.
        isSaveFromParentEnabled = false
    }

    /**
     * The view hosted by this holder.
     */
    var view: View? = null
        internal set(value) {
            if (value !== field) {
                field = value
                removeAllViews()
                if (value != null) {
                    addView(value)
                    runUpdate()
                }
            }
        }

    /**
     * The update logic of the [View].
     */
    var update: () -> Unit = {}
        protected set(value) {
            field = value
            hasUpdateBlock = true
            runUpdate()
        }
    private var hasUpdateBlock = false

    /**
     * The modifier of the `LayoutNode` corresponding to this [View].
     */
    var modifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke(value)
            }
        }

    internal var onModifierChanged: ((Modifier) -> Unit)? = null

    /**
     * The screen density of the layout.
     */
    var density: Density = Density(1f)
        set(value) {
            if (value !== field) {
                field = value
                onDensityChanged?.invoke(value)
            }
        }

    internal var onDensityChanged: ((Density) -> Unit)? = null

    /** Sets the [ViewTreeLifecycleOwner] for this view. */
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            if (value !== field) {
                field = value
                ViewTreeLifecycleOwner.set(this, value)
            }
        }

    /** Sets the ViewTreeSavedStateRegistryOwner for this view. */
    var savedStateRegistryOwner: SavedStateRegistryOwner? = null
        set(value) {
            if (value !== field) {
                field = value
                setViewTreeSavedStateRegistryOwner(value)
            }
        }

    private val snapshotObserver = SnapshotStateObserver { command ->
        if (handler.looper === Looper.myLooper()) {
            command()
        } else {
            handler.post(command)
        }
    }

    private val onCommitAffectingUpdate: (AndroidViewHolder) -> Unit = {
        handler.post(runUpdate)
    }

    private val runUpdate: () -> Unit = {
        if (hasUpdateBlock) {
            snapshotObserver.observeReads(this, onCommitAffectingUpdate, update)
        }
    }

    internal var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null

    private val location = IntArray(2)

    private var lastWidthMeasureSpec: Int = Unmeasured
    private var lastHeightMeasureSpec: Int = Unmeasured

    private val nestedScrollingParentHelper: NestedScrollingParentHelper =
        NestedScrollingParentHelper(this)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        view?.measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(view?.measuredWidth ?: 0, view?.measuredHeight ?: 0)
        lastWidthMeasureSpec = widthMeasureSpec
        lastHeightMeasureSpec = heightMeasureSpec
    }

    fun remeasure() {
        if (lastWidthMeasureSpec == Unmeasured || lastHeightMeasureSpec == Unmeasured) {
            // This should never happen: it means that the views handler was measured without
            // the AndroidComposeView having been measured.
            return
        }
        measure(lastWidthMeasureSpec, lastHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        view?.layout(0, 0, r - l, b - t)
    }

    override fun getLayoutParams(): LayoutParams? {
        return view?.layoutParams
            ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        snapshotObserver.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotObserver.stop()
        // remove all observations:
        snapshotObserver.clear()
    }

    // When there is no hardware acceleration invalidates are intercepted using this method,
    // otherwise using onDescendantInvalidated. Return null to avoid invalidating the
    // AndroidComposeView or the handler.
    @Suppress("Deprecation")
    override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): ViewParent? {
        super.invalidateChildInParent(location, dirty)
        layoutNode.invalidateLayer()
        return null
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        // We need to call super here in order to correctly update the dirty flags of the holder.
        super.onDescendantInvalidated(child, target)
        layoutNode.invalidateLayer()
    }

    // Always mark the region of the View to not be transparent to disable an optimisation which
    // would otherwise cause certain buggy drawing scenarios. For example, Compose drawing on top
    // of SurfaceViews included in Compose would sometimes not be displayed, as the drawing is
    // not done by Views, therefore the area is not known as non-transparent to the View system.
    override fun gatherTransparentRegion(region: Region?): Boolean {
        if (region == null) return true
        getLocationInWindow(location)
        region.op(
            location[0],
            location[1],
            location[0] + width,
            location[1] + height,
            Region.Op.DIFFERENCE
        )
        return true
    }

    /**
     * A [LayoutNode] tree representation for this Android [View] holder.
     * The [LayoutNode] will proxy the Compose core calls to the [View].
     */
    val layoutNode: LayoutNode = run {
        // Prepare layout node that proxies measure and layout passes to the View.
        val layoutNode = LayoutNode()

        val coreModifier = Modifier
            .pointerInteropFilter(this)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    (layoutNode.owner as? AndroidComposeView)
                        ?.drawAndroidView(this@AndroidViewHolder, canvas.nativeCanvas)
                }
            }.onGloballyPositioned {
                // The global position of this LayoutNode can change with it being replaced. For
                // these cases, we need to inform the View.
                layoutAccordingTo(layoutNode)
            }
        layoutNode.modifier = modifier.then(coreModifier)
        onModifierChanged = { layoutNode.modifier = it.then(coreModifier) }

        layoutNode.density = density
        onDensityChanged = { layoutNode.density = it }

        var viewRemovedOnDetach: View? = null
        layoutNode.onAttach = { owner ->
            (owner as? AndroidComposeView)?.addAndroidView(this, layoutNode)
            if (viewRemovedOnDetach != null) view = viewRemovedOnDetach
        }
        layoutNode.onDetach = { owner ->
            (owner as? AndroidComposeView)?.removeAndroidView(this)
            viewRemovedOnDetach = view
            view = null
        }

        layoutNode.measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                if (constraints.minWidth != 0) {
                    getChildAt(0).minimumWidth = constraints.minWidth
                }
                if (constraints.minHeight != 0) {
                    getChildAt(0).minimumHeight = constraints.minHeight
                }

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
                return layout(measuredWidth, measuredHeight) {
                    layoutAccordingTo(layoutNode)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = intrinsicWidth(height)

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ) = intrinsicWidth(height)

            private fun intrinsicWidth(height: Int): Int {
                measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    obtainMeasureSpec(0, height, layoutParams!!.height)
                )
                return measuredWidth
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = intrinsicHeight(width)

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ) = intrinsicHeight(width)

            private fun intrinsicHeight(width: Int): Int {
                measure(
                    obtainMeasureSpec(0, width, layoutParams!!.width),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                )
                return measuredHeight
            }
        }
        layoutNode
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
            MeasureSpec.makeMeasureSpec(preferred.coerceIn(min, max), MeasureSpec.EXACTLY)
        }
        preferred == LayoutParams.WRAP_CONTENT && max != Constraints.Infinity -> {
            // Wrap content layout param with finite max constraint. If max constraint is infinite,
            // we will measure the child with UNSPECIFIED.
            MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST)
        }
        preferred == LayoutParams.MATCH_PARENT && max != Constraints.Infinity -> {
            // Match parent layout param, so we force the child to fill the available space.
            MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY)
        }
        else -> {
            // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        }
    }

    // TODO: b/203141462 - consume whether the AndroidView() is inside a scrollable container, and
    //  use that to set this. In the meantime set true as the defensive default.
    override fun shouldDelayChildPressedState(): Boolean = true

    // NestedScrollingParent3
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0 ||
            (axes and ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0
    }

    override fun getNestedScrollAxes(): Int {
        return nestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollingParentHelper.onStopNestedScroll(target, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (!isNestedScrollingEnabled) return
        val consumedByParent =
            dispatcher.dispatchPostScroll(
                consumed = Offset(dxConsumed.toComposeOffset(), dyConsumed.toComposeOffset()),
                available = Offset(dxUnconsumed.toComposeOffset(), dyUnconsumed.toComposeOffset()),
                source = toNestedScrollSource(type)
            )
        consumed[0] = composeToViewOffset(consumedByParent.x)
        consumed[1] = composeToViewOffset(consumedByParent.y)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (!isNestedScrollingEnabled) return
        dispatcher.dispatchPostScroll(
            consumed = Offset(dxConsumed.toComposeOffset(), dyConsumed.toComposeOffset()),
            available = Offset(dxUnconsumed.toComposeOffset(), dyUnconsumed.toComposeOffset()),
            source = toNestedScrollSource(type)
        )
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!isNestedScrollingEnabled) return
        val consumedByParent =
            dispatcher.dispatchPreScroll(
                available = Offset(dx.toComposeOffset(), dy.toComposeOffset()),
                source = toNestedScrollSource(type)
            )
        consumed[0] = composeToViewOffset(consumedByParent.x)
        consumed[1] = composeToViewOffset(consumedByParent.y)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        if (!isNestedScrollingEnabled) return false
        val viewVelocity = Velocity(velocityX.toComposeVelocity(), velocityY.toComposeVelocity())
        dispatcher.coroutineScope.launch {
            if (!consumed) {
                dispatcher.dispatchPostFling(
                    consumed = Velocity.Zero,
                    available = viewVelocity
                )
            } else {
                dispatcher.dispatchPostFling(
                    consumed = viewVelocity,
                    available = Velocity.Zero
                )
            }
        }
        return false
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        if (!isNestedScrollingEnabled) return false
        val toBeConsumed = Velocity(velocityX.toComposeVelocity(), velocityY.toComposeVelocity())
        dispatcher.coroutineScope.launch {
            dispatcher.dispatchPreFling(toBeConsumed)
        }
        return false
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return view?.isNestedScrollingEnabled ?: super.isNestedScrollingEnabled()
    }
}

private fun View.layoutAccordingTo(layoutNode: LayoutNode) {
    val position = layoutNode.coordinates.positionInRoot()
    val x = position.x.roundToInt()
    val y = position.y.roundToInt()
    layout(x, y, x + measuredWidth, y + measuredHeight)
}

private const val Unmeasured = Int.MIN_VALUE

private fun Int.toComposeOffset() = toFloat() * -1

private fun Float.toComposeVelocity(): Float = this * -1f

private fun toNestedScrollSource(type: Int): NestedScrollSource = when (type) {
    ViewCompat.TYPE_TOUCH -> NestedScrollSource.Drag
    else -> NestedScrollSource.Fling
}