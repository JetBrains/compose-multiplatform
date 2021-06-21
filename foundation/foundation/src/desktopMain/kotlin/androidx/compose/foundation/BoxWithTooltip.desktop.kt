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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
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
 * @param offset Tooltip offset.
 * @param content Composable content that the current tooltip is set to.
 */
@Composable
fun BoxWithTooltip(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    delay: Int = 500,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val mousePosition = remember { mutableStateOf(IntOffset.Zero) }
    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job: Job? by remember { mutableStateOf(null) }

    fun startShowing() {
        job?.cancel()
        job = scope.launch {
            delay(delay.toLong())
            isVisible = true
        }
    }

    fun hide() {
        job?.cancel()
        isVisible = false
    }

    val popupPositionProvider = TooltipPositionProvider(
        point = mousePosition.value,
        offset = offset,
        density = density.density
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                val position = IntOffset(
                    coordinates.positionInWindow().x.toInt(),
                    coordinates.positionInWindow().y.toInt()
                )
                parentBounds = IntRect(position, size)
            }
            .pointerMoveFilter(
                onMove = {
                    mousePosition.value = IntOffset(
                        it.x.toInt() + parentBounds.left,
                        it.y.toInt() + parentBounds.top
                    )
                    false
                },
                onEnter = {
                    startShowing()
                    false
                },
                onExit = {
                    hide()
                    false
                }
            )
            .pointerInput(Unit) {
                detectDown {
                    hide()
                }
            },
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        content()
        if (isVisible) {
            Popup(
                popupPositionProvider = popupPositionProvider,
                onDismissRequest = { isVisible = false }
            ) {
                tooltip()
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

@Immutable
internal data class TooltipPositionProvider(
    val point: IntOffset,
    val offset: DpOffset,
    val density: Float
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val cursorRect = IntRect(
            point,
            IntSize(
                (DefaultCursorSize * density).toInt(),
                (DefaultCursorSize * density).toInt()
            )
        )
        val positionOffset = offset.toIntOffsetWithDensity(density)
        val tooltipMargin = (TooltipMargin * density).toInt()

        var x = when (layoutDirection) {
            LayoutDirection.Ltr -> {
                var result = cursorRect.left + positionOffset.x
                if (result + popupContentSize.width > windowSize.width - tooltipMargin) {
                    result -= popupContentSize.width
                }
                if (result < tooltipMargin) {
                    result = tooltipMargin
                }
                result
            }
            LayoutDirection.Rtl -> {
                var result = cursorRect.right + positionOffset.x - popupContentSize.width
                if (result < tooltipMargin) {
                    result += popupContentSize.width
                }
                if (result + popupContentSize.width > windowSize.width - tooltipMargin) {
                    result = windowSize.width - tooltipMargin - popupContentSize.width
                }
                result
            }
        }
        var y = cursorRect.bottom + positionOffset.y

        if (y + popupContentSize.height > windowSize.height - tooltipMargin) {
            y -= popupContentSize.height + cursorRect.height
        }
        if (y < tooltipMargin) {
            y = tooltipMargin
        }

        return IntOffset(x, y)
    }
}

private fun DpOffset.toIntOffsetWithDensity(density: Float) = IntOffset(
    (x.value * density).toInt(),
    (y.value * density).toInt(),
)
private val DefaultCursorSize = 16
private val TooltipMargin = 4