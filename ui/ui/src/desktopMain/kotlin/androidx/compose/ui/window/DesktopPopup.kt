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
import androidx.compose.runtime.rememberCompositionReference
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwnersAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

@Composable
internal actual fun ActualPopup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties?,
    content: @Composable () -> Unit
) {
    PopupLayout(popupPositionProvider, isFocusable, onDismissRequest, content)
}

@Composable
@Suppress("DEPRECATION") // tapGestureFilter
private fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val owners = DesktopOwnersAmbient.current
    val density = AmbientDensity.current

    val parentBounds = remember { mutableStateOf(IntRect.Zero) }

    // getting parent bounds
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            parentBounds.value = IntRect(
                coordinates.localToWindow(Offset.Zero).round(),
                coordinates.size
            )
        },
        measureBlock = { _, _ ->
            layout(0, 0) {}
        }
    )

    val parentComposition = rememberCompositionReference()
    val (owner, composition) = remember {
        val owner = DesktopOwner(owners, density)
        val composition = owner.setContent(parent = parentComposition) {
            Layout(
                content = content,
                modifier = Modifier.tapGestureFilter {
                    if (isFocusable) {
                        onDismissRequest?.invoke()
                    }
                },
                measureBlock = { measurables, constraints ->
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight

                    val windowSize = IntSize(
                        width = width,
                        height = height
                    )

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach {
                            val placeable = it.measure(constraints)
                            val offset = popupPositionProvider.calculatePosition(
                                anchorBounds = parentBounds.value,
                                windowSize = windowSize,
                                layoutDirection = layoutDirection,
                                popupContentSize = IntSize(placeable.width, placeable.height)
                            )
                            placeable.place(offset.x, offset.y)
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
