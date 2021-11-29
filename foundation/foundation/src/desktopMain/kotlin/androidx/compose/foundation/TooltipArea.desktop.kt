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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.rememberCursorPositionProvider
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Sets the tooltip for an element.
 *
 * @param tooltip Composable content of the tooltip.
 * @param modifier The modifier to be applied to the layout.
 * @param contentAlignment The default alignment inside the Box.
 * @param propagateMinConstraints Whether the incoming min constraints should be passed to content.
 * @param delay Delay in milliseconds.
 * @param tooltipPlacement Defines position of the tooltip.
 * @param content Composable content that the current tooltip is set to.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Deprecated(
    "Use TooltipArea",
    replaceWith = ReplaceWith(
        "TooltipArea(tooltip, modifier, delay, tooltipPlacement, content)"
    )
)
@Suppress("UNUSED_PARAMETER")
fun BoxWithTooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    delay: Int = 500,
    tooltipPlacement: TooltipPlacement = TooltipPlacement.CursorPoint(
        offset = DpOffset(0.dp, 16.dp)
    ),
    content: @Composable () -> Unit
) = TooltipArea(
    tooltip, modifier, delay, tooltipPlacement, content
)

/**
 * Sets the tooltip for an element.
 *
 * @param tooltip Composable content of the tooltip.
 * @param modifier The modifier to be applied to the layout.
 * @param delayMillis Delay in milliseconds.
 * @param tooltipPlacement Defines position of the tooltip.
 * @param content Composable content that the current tooltip is set to.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TooltipArea(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    delayMillis: Int = 500,
    tooltipPlacement: TooltipPlacement = TooltipPlacement.CursorPoint(
        offset = DpOffset(0.dp, 16.dp)
    ),
    content: @Composable () -> Unit
) {
    var parentBounds by remember { mutableStateOf(Rect.Zero) }
    var popupPosition by remember { mutableStateOf(Offset.Zero) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    fun startShowing() {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis.toLong())
            isVisible = true
        }
    }

    fun hide() {
        job?.cancel()
        isVisible = false
    }

    fun hideIfNotHovered(globalPosition: Offset) {
        if (!parentBounds.contains(globalPosition)) {
            hide()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { parentBounds = it.boundsInWindow() }
            .onPointerEvent(PointerEventType.Enter) { startShowing() }
            .onPointerEvent(PointerEventType.Move) {
                hideIfNotHovered(parentBounds.topLeft + it.position)
            }
            .onPointerEvent(PointerEventType.Exit) {
                hideIfNotHovered(parentBounds.topLeft + it.position)
            }
            .pointerInput(Unit) {
                detectDown {
                    hide()
                }
            }
    ) {
        content()
        if (isVisible) {
            @OptIn(ExperimentalFoundationApi::class)
            Popup(
                popupPositionProvider = tooltipPlacement.positionProvider(),
                onDismissRequest = { isVisible = false }
            ) {
                Box(
                    Modifier
                        .onGloballyPositioned { popupPosition = it.positionInWindow() }
                        .onPointerEvent(PointerEventType.Move) {
                            hideIfNotHovered(popupPosition + it.position)
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            hideIfNotHovered(popupPosition + it.position)
                        }
                ) {
                    tooltip()
                }
            }
        }
    }
}

private val PointerEvent.position get() = changes.first().position

private fun Modifier.onPointerEvent(
    eventType: PointerEventType,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
) = pointerInput(eventType, pass, onEvent) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pass)
            if (event.type == eventType) {
                onEvent(event)
            }
        }
    }
}

private suspend fun PointerInputScope.detectDown(onDown: (Offset) -> Unit) {
    while (true) {
        awaitPointerEventScope {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.find { it.changedToDown() }
            if (down != null) {
                onDown(down.position)
            }
        }
    }
}

/**
 * An interface for providing a [PopupPositionProvider] for the tooltip.
 */
@ExperimentalFoundationApi
interface TooltipPlacement {
    /**
     * Returns [PopupPositionProvider] implementation.
     */
    @Composable
    fun positionProvider(): PopupPositionProvider

    /**
     * [TooltipPlacement] implementation for providing a [PopupPositionProvider] that calculates
     * the position of the popup relative to the current mouse cursor position.
     *
     * @param offset [DpOffset] to be added to the position of the popup.
     * @param alignment The alignment of the popup relative to the current cursor position.
     * @param windowMargin Defines the area within the window that limits the placement of the popup.
     */
    @ExperimentalFoundationApi
    class CursorPoint(
        private val offset: DpOffset = DpOffset.Zero,
        private val alignment: Alignment = Alignment.BottomEnd,
        private val windowMargin: Dp = 4.dp
    ) : TooltipPlacement {
        @Composable
        override fun positionProvider() = rememberCursorPositionProvider(
            offset,
            alignment,
            windowMargin
        )
    }

    /**
     * [TooltipPlacement] implementation for providing a [PopupPositionProvider] that calculates
     * the position of the popup relative to the current component bounds.
     *
     * @param anchor The anchor point relative to the current component bounds.
     * @param alignment The alignment of the popup relative to the [anchor] point.
     * @param offset [DpOffset] to be added to the position of the popup.
     */
    @ExperimentalFoundationApi
    class ComponentRect(
        private val anchor: Alignment = Alignment.BottomCenter,
        private val alignment: Alignment = Alignment.BottomCenter,
        private val offset: DpOffset = DpOffset.Zero
    ) : TooltipPlacement {
        @Composable
        override fun positionProvider() = rememberComponentRectPositionProvider(
            anchor,
            alignment,
            offset
        )
    }
}
