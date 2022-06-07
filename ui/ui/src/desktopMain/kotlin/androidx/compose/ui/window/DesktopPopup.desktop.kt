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
package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalLayerContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import java.awt.MouseInfo
import javax.swing.SwingUtilities.convertPointFromScreen

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param focusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    focusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(
            alignment,
            offset
        )
    }

    Popup(
        popupPositionProvider = popupPositioner,
        onDismissRequest = onDismissRequest,
        onKeyEvent = onKeyEvent,
        onPreviewKeyEvent = onPreviewKeyEvent,
        focusable = focusable,
        content = content
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param focusable Indicates if the popup can grab the focus.
 * @param onPreviewKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. It gives ancestors of a focused component the chance to intercept a [KeyEvent].
 * Return true to stop propagation of this event. If you return false, the key event will be
 * sent to this [onPreviewKeyEvent]'s child. If none of the children consume the event,
 * it will be sent back up to the root using the onKeyEvent callback.
 * @param onKeyEvent This callback is invoked when the user interacts with the hardware
 * keyboard. While implementing this callback, return true to stop propagation of this event.
 * If you return false, the key event will be sent to this [onKeyEvent]'s parent.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    focusable: Boolean = false,
    content: @Composable () -> Unit
) {
    PopupLayout(
        popupPositionProvider,
        focusable,
        onDismissRequest,
        onPreviewKeyEvent,
        onKeyEvent,
        content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    focusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable () -> Unit
) {
    val scene = LocalComposeScene.current
    val density = LocalDensity.current

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    var popupBounds by remember { mutableStateOf(IntRect.Zero) }

    // getting parent bounds
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            parentBounds = IntRect(
                coordinates.localToWindow(Offset.Zero).round(),
                coordinates.size
            )
        },
        measurePolicy = { _, _ ->
            layout(0, 0) {}
        }
    )

    val parentComposition = rememberCompositionContext()
    val (owner, composition) = remember {
        val owner = SkiaBasedOwner(
            platformInputService = scene.platformInputService,
            component = scene.component,
            density = density,
            isPopup = true,
            isFocusable = focusable,
            onDismissRequest = onDismissRequest,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        scene.attach(owner)
        val composition = owner.setContent(parent = parentComposition) {
            Layout(
                content = content,
                measurePolicy = { measurables, constraints ->
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach {
                            val placeable = it.measure(constraints)
                            val position = popupPositionProvider.calculatePosition(
                                anchorBounds = parentBounds,
                                windowSize = IntSize(width, height),
                                layoutDirection = layoutDirection,
                                popupContentSize = IntSize(placeable.width, placeable.height)
                            )

                            popupBounds = IntRect(
                                position,
                                IntSize(placeable.width, placeable.height)
                            )
                            owner.bounds = popupBounds
                            placeable.place(position.x, position.y)
                        }
                    }
                }
            )
        }
        owner to composition
    }
    owner.density = density
    DisposableEffect(Unit) {
        onDispose {
            scene.detach(owner)
            composition.dispose()
            owner.dispose()
        }
    }
}

/**
 * Provides [PopupPositionProvider] relative to the current mouse cursor position.
 *
 * @param offset [DpOffset] to be added to the position of the popup.
 * @param alignment The alignment of the popup relative to the current cursor position.
 * @param windowMargin Defines the area within the window that limits the placement of the popup.
 */
@Composable
fun rememberCursorPositionProvider(
    offset: DpOffset = DpOffset.Zero,
    alignment: Alignment = Alignment.BottomEnd,
    windowMargin: Dp = 4.dp
): PopupPositionProvider = with(LocalDensity.current) {
    val component = LocalLayerContainer.current
    val cursorPoint = remember {
        val awtMousePosition = MouseInfo.getPointerInfo().location
        convertPointFromScreen(awtMousePosition, component)
        IntOffset(
            (awtMousePosition.x * component.density.density).toInt(),
            (awtMousePosition.y * component.density.density).toInt()
        )
    }
    val offsetPx = IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    val windowMarginPx = windowMargin.roundToPx()
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ) = with(density) {
            val anchor = IntRect(cursorPoint, IntSize.Zero)
            val tooltipArea = IntRect(
                IntOffset(
                    anchor.left - popupContentSize.width,
                    anchor.top - popupContentSize.height,
                ),
                IntSize(
                    popupContentSize.width * 2,
                    popupContentSize.height * 2
                )
            )
            val position = alignment.align(popupContentSize, tooltipArea.size, layoutDirection)
            var x = tooltipArea.left + position.x + offsetPx.x
            var y = tooltipArea.top + position.y + offsetPx.y
            if (x + popupContentSize.width > windowSize.width - windowMarginPx) {
                x -= popupContentSize.width
            }
            if (y + popupContentSize.height > windowSize.height - windowMarginPx) {
                y -= popupContentSize.height + anchor.height
            }
            if (x < windowMarginPx) {
                x = windowMarginPx
            }
            if (y < windowMarginPx) {
                y = windowMarginPx
            }
            IntOffset(x, y)
        }
    }
}

/**
 * Provides [PopupPositionProvider] relative to the current component bounds.
 *
 * @param anchor The anchor point relative to the current component bounds.
 * @param alignment The alignment of the popup relative to the [anchor] point.
 * @param offset [DpOffset] to be added to the position of the popup.
 */
@Composable
fun rememberComponentRectPositionProvider(
    anchor: Alignment = Alignment.BottomCenter,
    alignment: Alignment = Alignment.BottomCenter,
    offset: DpOffset = DpOffset.Zero
): PopupPositionProvider = with(LocalDensity.current) {
    val offsetPx = IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            val anchorPoint = anchor.align(IntSize.Zero, anchorBounds.size, layoutDirection)
            val tooltipArea = IntRect(
                IntOffset(
                    anchorBounds.left + anchorPoint.x - popupContentSize.width,
                    anchorBounds.top + anchorPoint.y - popupContentSize.height,
                ),
                IntSize(
                    popupContentSize.width * 2,
                    popupContentSize.height * 2
                )
            )
            val position = alignment.align(popupContentSize, tooltipArea.size, layoutDirection)
            return tooltipArea.topLeft + position + offsetPx
        }
    }
}
