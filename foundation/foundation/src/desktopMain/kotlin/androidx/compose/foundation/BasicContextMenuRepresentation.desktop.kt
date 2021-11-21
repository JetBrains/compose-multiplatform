/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider

// Design of basic represenation is from Material specs:
// https://material.io/design/interaction/states.html#hover
// https://material.io/components/menus#specs

val LightDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color.White,
    textColor = Color.Black,
    itemHoverColor = Color.Black.copy(alpha = 0.04f)
)

val DarkDefaultContextMenuRepresentation = DefaultContextMenuRepresentation(
    backgroundColor = Color(0xFF121212), // like surface in darkColors
    textColor = Color.White,
    itemHoverColor = Color.White.copy(alpha = 0.04f)
)

class DefaultContextMenuRepresentation(
    private val backgroundColor: Color,
    private val textColor: Color,
    private val itemHoverColor: Color
) : ContextMenuRepresentation {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: List<ContextMenuItem>) {
        val isOpen = state.status is ContextMenuState.Status.Open
        if (isOpen) {
            Popup(
                focusable = true,
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
                popupPositionProvider = rememberCursorPositionProvider(),
                onKeyEvent = {
                    if (it.key == Key.Escape) {
                        state.status = ContextMenuState.Status.Closed
                        true
                    } else {
                        false
                    }
                },
            ) {
                Column(
                    modifier = Modifier
                        .shadow(8.dp)
                        .background(backgroundColor)
                        .padding(vertical = 4.dp)
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())

                ) {
                    items.distinctBy { it.label }.forEach { item ->
                        MenuItemContent(
                            itemHoverColor = itemHoverColor,
                            onClick = {
                                state.status = ContextMenuState.Status.Closed
                                item.onClick()
                            }
                        ) {
                            BasicText(text = item.label, style = TextStyle(color = textColor))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemContent(
    itemHoverColor: Color,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clickable(
                onClick = onClick,
            )
            .onHover { hovered = it }
            .background(if (hovered) itemHoverColor else Color.Transparent)
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .sizeIn(
                minWidth = 112.dp,
                maxWidth = 280.dp,
                minHeight = 32.dp
            )
            .padding(
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 0.dp
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

private fun Modifier.onHover(onHover: (Boolean) -> Unit) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Enter -> onHover(true)
                PointerEventType.Exit -> onHover(false)
            }
        }
    }
}