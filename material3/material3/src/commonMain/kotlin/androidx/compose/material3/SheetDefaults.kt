/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SheetValue.Collapsed
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.tokens.ScrimTokens
import androidx.compose.material3.tokens.SheetBottomTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException

/**
 * Create and [remember] a [SheetState].
 *
 * @param skipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberSheetState(
    skipHalfExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true }
): SheetState {
    return rememberSaveable(
        skipHalfExpanded, confirmValueChange,
        saver = SheetState.Saver(
            skipHalfExpanded = skipHalfExpanded,
            confirmValueChange = confirmValueChange
        )
    ) {
        SheetState(skipHalfExpanded, confirmValueChange = confirmValueChange)
    }
}

/**
 * State of a sheet composable, such as [ModalBottomSheet]
 *
 * Contains states relating to it's swipe position as well as animations between state values.
 *
 * @param skipCollapsed Whether the collapsed state, if the bottom sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
@ExperimentalMaterial3Api
class SheetState(
    internal val skipCollapsed: Boolean,
    initialValue: SheetValue = Hidden,
    confirmValueChange: (SheetValue) -> Boolean = { true }
) {
    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the state the bottom sheet is
     * currently in. If a swipe or an animation is in progress, this corresponds the state the sheet
     * was in before the swipe or animation started.
     */
    val currentValue: SheetValue get() = swipeableState.currentValue

    /**
     * The target value of the bottom sheet state.
     *
     * If a swipe is in progress, this is the value that the sheet would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: SheetValue get() = swipeableState.targetValue

    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = swipeableState.currentValue != Hidden

    /**
     * Require the current offset (in pixels) of the bottom sheet.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = swipeableState.requireOffset()

    /**
     * Show the bottom sheet with animation and suspend until it's shown. If the sheet is taller
     * than 50% of the parent's height, the bottom sheet will be half expanded. Otherwise it will be
     * fully expanded.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun show() {
        val targetValue = when {
            hasCollapsedState -> Collapsed
            else -> Expanded
        }
        swipeableState.animateTo(targetValue)
    }

    /**
     * Hide the bottom sheet with animation and suspend until it is fully hidden or animation has
     * been cancelled.
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun hide() {
        swipeableState.animateTo(Hidden)
    }

    /**
     * Hide the bottom sheet with animation and suspend until it is collapsed or animation has
     * been cancelled.
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun collapse() {
        swipeableState.animateTo(Collapsed)
    }

    companion object {
        /**
         * The default [Saver] implementation for [SheetState].
         */
        fun Saver(
            skipHalfExpanded: Boolean,
            confirmValueChange: (SheetValue) -> Boolean
        ) = Saver<SheetState, SheetValue>(
            save = { it.currentValue },
            restore = { SheetState(skipHalfExpanded, it, confirmValueChange) }
        )
    }

    internal var swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = SwipeableV2Defaults.AnimationSpec,
        confirmValueChange = confirmValueChange,
    )

    internal val hasCollapsedState: Boolean
        get() = swipeableState.hasAnchorForValue(Collapsed)

    internal val hasExpandedState: Boolean
        get() = swipeableState.hasAnchorForValue(Expanded)

    internal val offset: Float? get() = swipeableState.offset

    /**
     * Fully expand the bottom sheet with animation and suspend until it if fully expanded or
     * animation has been cancelled.
     * *
     * @throws [CancellationException] if the animation is interrupted
     */
    internal suspend fun expand() {
        swipeableState.animateTo(Expanded)
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(velocity: Float) {
        swipeableState.settle(velocity)
    }
}

/**
 * Possible values of [SheetState].
 */
@ExperimentalMaterial3Api
enum class SheetValue {
    /**
     * The sheet is not visible.
     */
    Hidden,

    /**
     * The sheet is visible at full height.
     */
    Expanded,

    /**
     * The sheet is partially visible.
     */
    Collapsed,
}

/**
 * Contains the default values used by [ModalBottomSheet].
 */
@Stable
@ExperimentalMaterial3Api
object BottomSheetDefaults {
    /** The default shape for a [ModalBottomSheet] in a [Hidden] state. */
    val MinimizedShape: Shape
        @Composable get() =
        SheetBottomTokens.DockedMinimizedContainerShape.toShape()

    /** The default shape for a [ModalBottomSheet] in [Collapsed] and [Expanded] states. */
    val ExpandedShape: Shape
        @Composable get() =
        SheetBottomTokens.DockedContainerShape.toShape()

    /** The default container color for a bottom sheet. */
    val ContainerColor: Color
        @Composable get() =
        SheetBottomTokens.DockedContainerColor.toColor()

    /** The default elevation for a bottom sheet. */
    val Elevation = SheetBottomTokens.DockedModalContainerElevation

    /** The default color of the scrim overlay for background content. */
    val ScrimColor: Color
        @Composable get() =
        ScrimTokens.ContainerColor.toColor().copy(ScrimTokens.ContainerOpacity)

    @Composable
    fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = SheetBottomTokens.DockedDragHandleWidth,
        height: Dp = SheetBottomTokens.DockedDragHandleHeight,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        color: Color = SheetBottomTokens.DockedDragHandleColor.toColor()
            .copy(SheetBottomTokens.DockedDragHandleOpacity),
    ) {
        Surface(
            modifier = modifier.padding(vertical = DragHandleVerticalPadding),
            color = color,
            shape = shape
        ) {
            Box(
                Modifier
                    .size(
                        width = width,
                        height = height
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: SheetState,
    orientation: Orientation,
    onFling: (velocity: Float) -> Unit
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.Drag) {
            sheetState.swipeableState.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return if (source == NestedScrollSource.Drag) {
            sheetState.swipeableState.dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        val currentOffset = sheetState.requireOffset()
        return if (toFling < 0 && currentOffset > sheetState.swipeableState.minBound) {
            onFling(toFling)
            // since we go to the anchor with tween settling, consume all for the best UX
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        onFling(available.toFloat())
        return available
    }

    private fun Float.toOffset(): Offset = Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f
    )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}

private val DragHandleVerticalPadding = 22.dp