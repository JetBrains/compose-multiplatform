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

package androidx.compose.material

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.nestedscroll.nestedScroll
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Possible values of [ModalBottomSheetState].
 */
@ExperimentalMaterialApi
enum class ModalBottomSheetValue {
    /**
     * The bottom sheet is not visible.
     */
    Hidden,

    /**
     * The bottom sheet is visible at full height.
     */
    Expanded,

    /**
     * The bottom sheet is partially visible at 50% of the screen height. This state is only
     * enabled if the height of the bottom sheet is more than 50% of the screen height.
     */
    HalfExpanded
}

/**
 * State of the [ModalBottomSheetLayout] composable.
 *
 * @param initialValue The initial value of the state.
 * @param clock The animation clock that will be used to drive the animations.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
class ModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    clock: AnimationClockObservable,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
) : SwipeableState<ModalBottomSheetValue>(
    initialValue = initialValue,
    clock = clock,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange
) {
    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = value != ModalBottomSheetValue.Hidden

    private val isHalfExpandedEnabled: Boolean
        get() = anchors.values.contains(ModalBottomSheetValue.HalfExpanded)

    /**
     * Show the bottom sheet, with an animation.
     *
     * @param onShown Optional callback invoked when the bottom sheet has been shown.
     */
    fun show(onShown: (() -> Unit)? = null) {
        val targetValue =
            if (isHalfExpandedEnabled) ModalBottomSheetValue.HalfExpanded
            else ModalBottomSheetValue.Expanded
        animateTo(
            targetValue = targetValue,
            onEnd = { endReason, _ ->
                @Suppress("Deprecation")
                if (endReason == AnimationEndReason.TargetReached) {
                    onShown?.invoke()
                }
            }
        )
    }

    /**
     * Hide the bottom sheet, with an animation.
     *
     * @param onHidden Optional callback invoked when the bottom sheet has been hidden.
     */
    fun hide(onHidden: (() -> Unit)? = null) {
        animateTo(
            targetValue = ModalBottomSheetValue.Hidden,
            onEnd = { endReason, _ ->
                @Suppress("Deprecation")
                if (endReason == AnimationEndReason.TargetReached) {
                    onHidden?.invoke()
                }
            }
        )
    }

    internal val nestedScrollConnection = this.PreUpPostDownNestedScrollConnection

    companion object {
        /**
         * The default [Saver] implementation for [ModalBottomSheetState].
         */
        fun Saver(
            clock: AnimationClockObservable,
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (ModalBottomSheetValue) -> Boolean
        ): Saver<ModalBottomSheetState, *> = Saver(
            save = { it.value },
            restore = {
                ModalBottomSheetState(
                    initialValue = it,
                    clock = clock,
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange
                )
            }
        )
    }
}

/**
 * Create a [ModalBottomSheetState] and [remember] it against the [clock]. If a clock is not
 * specified, the default animation clock will be used, as provided by [AnimationClockAmbient].
 *
 * @param initialValue The initial value of the state.
 * @param clock The animation clock that will be used to drive the animations.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    clock: AnimationClockObservable = AmbientAnimationClock.current,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
): ModalBottomSheetState {
    val disposableClock = clock.asDisposableClock()
    return rememberSavedInstanceState(
        disposableClock,
        saver = ModalBottomSheetState.Saver(
            clock = disposableClock,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        ModalBottomSheetState(
            initialValue = initialValue,
            clock = disposableClock,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    }
}

/**
 * Modal bottom sheets present a set of choices while blocking interaction with the rest of the
 * screen. They are an alternative to inline menus and simple dialogs on mobile, providing
 * additional room for content, iconography, and actions.
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material.samples.ModalBottomSheetSample
 *
 * @param sheetContent The content of the bottom sheet.
 * @param modifier Optional [Modifier] for the entire component.
 * @param sheetState The state of the bottom sheet.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 * children. Defaults to the matching `onFoo` color for [sheetBackgroundColor], or if that is not
 * a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param scrimColor The color of the scrim that is applied to the rest of the screen when the
 * bottom sheet is visible. If you set this to `Color.Transparent`, then a scrim will no longer be
 * applied and the bottom sheet will not block interaction with the rest of the screen when visible.
 * @param content The content of rest of the screen.
 */
@Composable
@ExperimentalMaterialApi
fun ModalBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    content: @Composable () -> Unit
) = BottomSheetStack(
    modifier = modifier,
    sheetContent = {
        Surface(
            Modifier
                .fillMaxWidth()
                .nestedScroll(sheetState.nestedScrollConnection)
                .offset { IntOffset(0, sheetState.offset.value.roundToInt()) },
            shape = sheetShape,
            elevation = sheetElevation,
            color = sheetBackgroundColor,
            contentColor = sheetContentColor
        ) {
            Column(content = sheetContent)
        }
    },
    content = { constraints, sheetHeight ->
        val fullHeight = constraints.maxHeight.toFloat()
        val anchors = if (sheetHeight < fullHeight / 2) {
            mapOf(
                fullHeight to ModalBottomSheetValue.Hidden,
                fullHeight - sheetHeight to ModalBottomSheetValue.Expanded
            )
        } else {
            mapOf(
                fullHeight to ModalBottomSheetValue.Hidden,
                fullHeight / 2 to ModalBottomSheetValue.HalfExpanded,
                max(0f, fullHeight - sheetHeight) to ModalBottomSheetValue.Expanded
            )
        }
        val swipeable = Modifier.swipeable(
            state = sheetState,
            anchors = anchors,
            orientation = Orientation.Vertical,
            enabled = sheetState.value != ModalBottomSheetValue.Hidden,
            resistance = null
        )

        Box(Modifier.fillMaxSize().then(swipeable)) {
            content()

            Scrim(
                color = scrimColor,
                onDismiss = { sheetState.hide() },
                visible = sheetState.targetValue != ModalBottomSheetValue.Hidden
            )
        }
    }
)

@Composable
private fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color != Color.Transparent) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val dismissModifier = if (visible) {
            Modifier.pointerInput { detectTapGestures { onDismiss() } }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

@Composable
private fun BottomSheetStack(
    modifier: Modifier,
    sheetContent: @Composable () -> Unit,
    content: @Composable (constraints: Constraints, sheetHeight: Float) -> Unit
) {
    SubcomposeLayout(modifier) { constraints ->
        val sheetPlaceable =
            subcompose(BottomSheetStackSlot.SheetContent, sheetContent)
                .first().measure(constraints.copy(minWidth = 0, minHeight = 0))

        val sheetHeight = sheetPlaceable.height.toFloat()

        val placeable =
            subcompose(BottomSheetStackSlot.Content) { content(constraints, sheetHeight) }
                .first().measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
            sheetPlaceable.placeRelative(0, 0)
        }
    }
}

private enum class BottomSheetStackSlot { SheetContent, Content }

/**
 * Contains useful Defaults for [ModalBottomSheetLayout].
 */
object ModalBottomSheetDefaults {

    /**
     * The default elevation used by [ModalBottomSheetLayout].
     */
    val Elevation = 16.dp

    /**
     * The default scrim color used by [ModalBottomSheetLayout].
     */
    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
}
