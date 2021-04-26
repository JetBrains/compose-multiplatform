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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@ExperimentalMaterialApi
class ModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
) : SwipeableState<ModalBottomSheetValue>(
    initialValue = initialValue,
    animationSpec = animationSpec,
    confirmStateChange = confirmStateChange
) {
    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = currentValue != ModalBottomSheetValue.Hidden

    internal val isHalfExpandedEnabled: Boolean
        get() = anchors.values.contains(ModalBottomSheetValue.HalfExpanded)

    /**
     * Show the bottom sheet with animation and suspend until it's shown. If half expand is
     * enabled, the bottom sheet will be half expanded. Otherwise it will be fully expanded.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun show() {
        val targetValue =
            if (isHalfExpandedEnabled) ModalBottomSheetValue.HalfExpanded
            else ModalBottomSheetValue.Expanded
        animateTo(targetValue = targetValue)
    }

    /**
     * Half expand the bottom sheet if half expand is enabled with animation and suspend until it
     * animation is complete or cancelled
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    internal suspend fun halfExpand() {
        if (!isHalfExpandedEnabled) {
            return
        }
        animateTo(ModalBottomSheetValue.HalfExpanded)
    }

    /**
     * Fully expand the bottom sheet with animation and suspend until it if fully expanded or
     * animation has been cancelled.
     * *
     * @throws [CancellationException] if the animation is interrupted
     */
    internal suspend fun expand() = animateTo(ModalBottomSheetValue.Expanded)

    /**
     * Hide the bottom sheet with animation and suspend until it if fully hidden or animation has
     * been cancelled.
     *
     * @throws [CancellationException] if the animation is interrupted
     */
    suspend fun hide() = animateTo(ModalBottomSheetValue.Hidden)

    internal val nestedScrollConnection = this.PreUpPostDownNestedScrollConnection

    companion object {
        /**
         * The default [Saver] implementation for [ModalBottomSheetState].
         */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (ModalBottomSheetValue) -> Boolean
        ): Saver<ModalBottomSheetState, *> = Saver(
            save = { it.currentValue },
            restore = {
                ModalBottomSheetState(
                    initialValue = it,
                    animationSpec = animationSpec,
                    confirmStateChange = confirmStateChange
                )
            }
        )
    }
}

/**
 * Create a [ModalBottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmStateChange: (ModalBottomSheetValue) -> Boolean = { true }
): ModalBottomSheetState {
    return rememberSaveable(
        saver = ModalBottomSheetState.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        ModalBottomSheetState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    }
}

/**
 * <a href="https://material.io/components/sheets-bottom#modal-bottom-sheet" class="external" target="_blank">Material Design modal bottom sheet</a>.
 *
 * Modal bottom sheets present a set of choices while blocking interaction with the rest of the
 * screen. They are an alternative to inline menus and simple dialogs, providing
 * additional room for content, iconography, and actions.
 *
 * ![Modal bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material/modal-bottom-sheet.png)
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
 * children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
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
) {
    val scope = rememberCoroutineScope()
    BottomSheetStack(
        modifier = modifier,
        sheetContent = {
            Surface(
                Modifier
                    .fillMaxWidth()
                    .nestedScroll(sheetState.nestedScrollConnection)
                    .offset { IntOffset(0, sheetState.offset.value.roundToInt()) }
                    .semantics {
                        if (sheetState.isVisible) {
                            dismiss {
                                scope.launch { sheetState.hide() }
                                true
                            }
                            if (sheetState.currentValue == ModalBottomSheetValue.HalfExpanded) {
                                expand {
                                    scope.launch { sheetState.expand() }
                                    true
                                }
                            } else if (sheetState.isHalfExpandedEnabled) {
                                collapse {
                                    scope.launch { sheetState.halfExpand() }
                                    true
                                }
                            }
                        }
                    },
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
                enabled = sheetState.currentValue != ModalBottomSheetValue.Hidden,
                resistance = null
            )

            Box(Modifier.fillMaxSize().then(swipeable)) {
                content()
                Scrim(
                    color = scrimColor,
                    onDismiss = { scope.launch { sheetState.hide() } },
                    visible = sheetState.targetValue != ModalBottomSheetValue.Hidden
                )
            }
        }
    )
}

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
            Modifier
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    contentDescription = Strings.CloseSheet
                    onClick { onDismiss(); true }
                }
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
