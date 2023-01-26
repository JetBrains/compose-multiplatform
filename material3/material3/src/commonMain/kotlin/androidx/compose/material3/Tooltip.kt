/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: add link to m3 doc once created by designer at the top
/**
 * Plain tooltip that provides a descriptive message for an anchor.
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
 *
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param tooltipState handles the state of the tooltip's visibility.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
@ExperimentalMaterial3Api
fun PlainTooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tooltipState: TooltipState = remember { TooltipState() },
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable TooltipBoxScope.() -> Unit
) {
    val tooltipAnchorPadding = with(LocalDensity.current) { TooltipAnchorPadding.roundToPx() }
    val positionProvider = remember { PlainTooltipPositionProvider(tooltipAnchorPadding) }

    TooltipBox(
        tooltipContent = {
            PlainTooltipImpl(
                textColor = contentColor,
                content = tooltip
            )
        },
        modifier = modifier,
        tooltipState = tooltipState,
        shape = shape,
        containerColor = containerColor,
        tooltipPositionProvider = positionProvider,
        elevation = 0.dp,
        maxWidth = PlainTooltipMaxWidth,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipBox(
    tooltipContent: @Composable () -> Unit,
    tooltipPositionProvider: PopupPositionProvider,
    modifier: Modifier,
    shape: Shape,
    tooltipState: TooltipState,
    containerColor: Color,
    elevation: Dp,
    maxWidth: Dp,
    content: @Composable TooltipBoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val longPressLabel = getString(string = Strings.TooltipLongPressLabel)

    val scope = remember {
        object : TooltipBoxScope {
            override fun Modifier.tooltipAnchor(): Modifier {
                val onLongPress = {
                    coroutineScope.launch {
                        tooltipState.show()
                    }
                }
                return pointerInput(tooltipState) {
                        awaitEachGesture {
                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                            val pass = PointerEventPass.Initial

                            // wait for the first down press
                            awaitFirstDown(pass = pass)

                            try {
                                // listen to if there is up gesture within the longPressTimeout limit
                                withTimeout(longPressTimeout) {
                                    waitForUpOrCancellation(pass = pass)
                                }
                            } catch (_: PointerEventTimeoutCancellationException) {
                                // handle long press - Show the tooltip
                                onLongPress()

                                // consume the children's click handling
                                val event = awaitPointerEvent(pass = pass)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }.semantics(mergeDescendants = true) {
                        onLongClick(
                            label = longPressLabel,
                            action = {
                                onLongPress()
                                true
                            }
                        )
                    }
            }
        }
    }

    Box {
        Popup(
            popupPositionProvider = tooltipPositionProvider,
            onDismissRequest = {
                if (tooltipState.isVisible) {
                    coroutineScope.launch { tooltipState.dismiss() }
                }
            }
        ) {
            Surface(
                modifier = modifier
                    .sizeIn(
                        minWidth = TooltipMinWidth,
                        maxWidth = maxWidth,
                        minHeight = TooltipMinHeight
                    )
                    .animateTooltip(tooltipState.isVisible)
                    .focusable()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = shape,
                color = containerColor,
                shadowElevation = elevation,
                content = tooltipContent
            )
        }

        scope.content()
    }
}

@Composable
private fun PlainTooltipImpl(
    textColor: Color,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(PlainTooltipContentPadding)) {
        val textStyle = MaterialTheme.typography.fromToken(PlainTooltipTokens.SupportingTextFont)
        CompositionLocalProvider(
            LocalContentColor provides textColor,
            LocalTextStyle provides textStyle,
            content = content
        )
    }
}

/**
 * Tooltip defaults that contain default values for both [PlainTooltipBox] and RichTooltipBox
 */
@ExperimentalMaterial3Api
object TooltipDefaults {
    /**
     * The default [Shape] for the tooltip's container.
     */
    val plainTooltipContainerShape: Shape
        @Composable get() = PlainTooltipTokens.ContainerShape.toShape()

    /**
     * The default [Color] for the tooltip's container.
     */
    val plainTooltipContainerColor: Color
        @Composable get() = PlainTooltipTokens.ContainerColor.toColor()

    /**
     * The default [color] for the content within the tooltip.
     */
    val plainTooltipContentColor: Color
        @Composable get() = PlainTooltipTokens.SupportingTextColor.toColor()
}

private class PlainTooltipPositionProvider(
    val tooltipAnchorPadding: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2

        // Tooltip prefers to be above the anchor,
        // but if this causes the tooltip to overlap with the anchor
        // then we place it below the anchor
        var y = anchorBounds.top - popupContentSize.height - tooltipAnchorPadding
        if (y < 0)
            y = anchorBounds.bottom + tooltipAnchorPadding
        return IntOffset(x, y)
    }
}

private fun Modifier.animateTooltip(
    showTooltip: Boolean
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animateTooltip"
        properties["showTooltip"] = showTooltip
    }
) {
    val transition = updateTransition(showTooltip, label = "Tooltip transition")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // show tooltip
                tween(
                    durationMillis = TooltipFadeInDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // dismiss tooltip
                tween(
                    durationMillis = TooltipFadeOutDuration,
                    easing = LinearOutSlowInEasing
                )
            }
        },
        label = "tooltip transition: scaling"
    ) { if (it) 1f else 0.8f }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // show tooltip
                tween(
                    durationMillis = TooltipFadeInDuration,
                    easing = LinearEasing
                )
            } else {
                // dismiss tooltip
                tween(
                    durationMillis = TooltipFadeOutDuration,
                    easing = LinearEasing
                )
            }
        },
        label = "tooltip transition: alpha"
    ) { if (it) 1f else 0f }

    this.graphicsLayer(
        scaleX = scale,
        scaleY = scale,
        alpha = alpha
    )
}

/**
 * Scope of [PlainTooltipBox] and RichTooltipBox
 */
@ExperimentalMaterial3Api
interface TooltipBoxScope {
    /**
     * [Modifier] that should be applied to the anchor composable when showing the tooltip
     * after long pressing the anchor composable is desired. It appends a long click to
     * the composable that this modifier is chained with.
     */
    fun Modifier.tooltipAnchor(): Modifier
}

/**
 * The state that is associated with an instance of a tooltip.
 * Each instance of tooltips should have its own [TooltipState]
 * while will be used to synchronize the tooltips shown.
 */
@Stable
@ExperimentalMaterial3Api
class TooltipState {
    /**
     * [Boolean] that will be used to update the visibility
     * state of the associated tooltip.
     */
    var isVisible by mutableStateOf(false)
        private set

    /**
     * Show the tooltip associated with the current [TooltipState].
     */
    suspend fun show() { show(this) }

    /**
     * Dismiss the tooltip associated with
     * this [TooltipState] if it's currently being shown.
     */
    suspend fun dismiss() {
        if (this == mutexOwner)
            dismissCurrentTooltip()
    }

    /**
     * Companion object used to synchronize
     * multiple [TooltipState]s, ensuring that there will
     * only be one tooltip shown on the screen at any given time.
     */
    private companion object {
        val mutatorMutex: MutatorMutex = MutatorMutex()
        var mutexOwner: TooltipState? = null

        /**
         * Shows the tooltip associated with [TooltipState],
         * it dismisses any tooltip currently being shown.
         */
        suspend fun show(
            state: TooltipState
        ) {
            mutatorMutex.mutate(MutatePriority.Default) {
                try {
                    mutexOwner = state
                    // show the tooltip associated with the
                    // tooltipState until dismissal or timeout.
                    state.isVisible = true
                    delay(TooltipDuration)
                } finally {
                    mutexOwner = null
                    // timeout or cancellation has occurred
                    // and we close out the current tooltip.
                    state.isVisible = false
                }
            }
        }

        /**
         * Dismisses the tooltip currently
         * being shown by freeing up the lock.
         */
        suspend fun dismissCurrentTooltip() {
            mutatorMutex.mutate(MutatePriority.UserInput) {
                /* Do nothing, we're just freeing up the mutex */
            }
        }
    }
}

private val TooltipAnchorPadding = 4.dp
internal val TooltipMinHeight = 24.dp
internal val TooltipMinWidth = 40.dp
private val PlainTooltipMaxWidth = 200.dp
private val PlainTooltipContentPadding = PaddingValues(8.dp, 4.dp)
internal const val TooltipDuration = 1500L
// No specification for fade in and fade out duration, so aligning it with the behavior for snackbar
private const val TooltipFadeInDuration = 150
private const val TooltipFadeOutDuration = 75