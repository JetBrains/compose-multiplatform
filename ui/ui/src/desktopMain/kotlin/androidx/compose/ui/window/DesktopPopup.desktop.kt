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

import androidx.compose.desktop.LocalLayerContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalDesktopOwners
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

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
 * @property contextMenu Places the popup window below the lower-right rectangle of the mouse
 * cursor image (basic context menu behaviour).
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
    contextMenu: Boolean = false,
    content: @Composable () -> Unit
) {
    PopupLayout(
        popupPositionProvider,
        focusable,
        contextMenu,
        onDismissRequest,
        onPreviewKeyEvent,
        onKeyEvent,
        content
    )
}

@Composable
private fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    focusable: Boolean,
    contextMenu: Boolean,
    onDismissRequest: (() -> Unit)?,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false },
    content: @Composable () -> Unit
) {
    val owners = LocalDesktopOwners.current
    val density = LocalDensity.current
    val component = if (contextMenu) LocalLayerContainer.current else null

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    var popupBounds by remember { mutableStateOf(IntRect.Zero) }
    val pointClick = remember { component?.getMousePosition() }

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
        val owner = DesktopOwner(
            container = owners,
            density = density,
            isPopup = true,
            isFocusable = focusable,
            onDismissRequest = onDismissRequest,
            onPreviewKeyEvent = onPreviewKeyEvent,
            onKeyEvent = onKeyEvent
        )
        val composition = owner.setContent(parent = parentComposition) {
            Layout(
                content = content,
                measurePolicy = { measurables, constraints ->
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight

                    val windowSize = IntSize(
                        width = width,
                        height = height
                    )

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach {
                            val placeable = it.measure(constraints)
                            var position: IntOffset
                            if (contextMenu) {
                                position = IntOffset(
                                    (pointClick!!.x * density.density).toInt(),
                                    (pointClick.y * density.density).toInt()
                                )
                            } else {
                                position = popupPositionProvider.calculatePosition(
                                    anchorBounds = parentBounds,
                                    windowSize = windowSize,
                                    layoutDirection = layoutDirection,
                                    popupContentSize = IntSize(placeable.width, placeable.height)
                                )
                            }
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
            composition.dispose()
            owner.dispose()
        }
    }
}

private fun isOutsideRectTap(rect: IntRect, point: Offset): Boolean {
    return !rect.contains(IntOffset(point.x.toInt(), point.y.toInt()))
}
