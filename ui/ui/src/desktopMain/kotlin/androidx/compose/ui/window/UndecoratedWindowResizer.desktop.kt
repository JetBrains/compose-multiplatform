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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LayoutWithWorkaround
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.Dimension
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

internal val DefaultBorderThickness = 8.dp

internal class UndecoratedWindowResizer(
    private val window: Window,
    var borderThickness: Dp = DefaultBorderThickness
) {
    var enabled: Boolean by mutableStateOf(false)

    private var initialPointPos = Point()
    private var initialWindowPos = Point()
    private var initialWindowSize = Dimension()

    @Composable
    fun Content() {
        if (enabled) {
            LayoutWithWorkaround(
                {
                    Side(Cursor.W_RESIZE_CURSOR, Side.Left)
                    Side(Cursor.E_RESIZE_CURSOR, Side.Right)
                    Side(Cursor.N_RESIZE_CURSOR, Side.Top)
                    Side(Cursor.S_RESIZE_CURSOR, Side.Bottom)
                    Side(Cursor.NW_RESIZE_CURSOR, Side.Left or Side.Top)
                    Side(Cursor.NE_RESIZE_CURSOR, Side.Right or Side.Top)
                    Side(Cursor.SW_RESIZE_CURSOR, Side.Left or Side.Bottom)
                    Side(Cursor.SE_RESIZE_CURSOR, Side.Right or Side.Bottom)
                },
                Modifier,
                measurePolicy = { measurables, constraints ->
                    val b = borderThickness.roundToPx()
                    fun Measurable.measureSide(width: Int, height: Int) = measure(
                        Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0))
                    )

                    val left = measurables[0].measureSide(b, constraints.maxHeight - 2 * b)
                    val right = measurables[1].measureSide(b, constraints.maxHeight - 2 * b)
                    val top = measurables[2].measureSide(constraints.maxWidth - 2 * b, b)
                    val bottom = measurables[3].measureSide(constraints.maxWidth - 2 * b, b)
                    val leftTop = measurables[4].measureSide(b, b)
                    val rightTop = measurables[5].measureSide(b, b)
                    val leftBottom = measurables[6].measureSide(b, b)
                    val rightBottom = measurables[7].measureSide(b, b)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        left.place(0, b)
                        right.place(constraints.maxWidth - b, b)
                        top.place(b, 0)
                        bottom.place(0, constraints.maxHeight - b)
                        leftTop.place(0, 0)
                        rightTop.place(constraints.maxWidth - b, 0)
                        leftBottom.place(0, constraints.maxHeight - b)
                        rightBottom.place(constraints.maxWidth - b, constraints.maxHeight - b)
                    }
                }
            )
        }
    }

    private fun Modifier.resizeOnDrag(sides: Int) = pointerInput(Unit) {
        var isResizing = false
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.first()
                val changedToPressed = !change.previousPressed && change.pressed

                if (event.buttons.isPrimaryPressed && changedToPressed) {
                    initialPointPos = MouseInfo.getPointerInfo().location
                    initialWindowPos = Point(window.x, window.y)
                    initialWindowSize = Dimension(window.width, window.height)
                    isResizing = true
                }

                if (!event.buttons.isPrimaryPressed) {
                    isResizing = false
                }

                if (event.type == PointerEventType.Move) {
                    if (isResizing) {
                        resize(sides)
                    }
                }
            }
        }
    }

    @Composable
    private fun Side(cursorId: Int, sides: Int) = LayoutWithWorkaround(
        {},
        Modifier.cursor(cursorId).resizeOnDrag(sides),
        measurePolicy = { _, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {}
        }
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun Modifier.cursor(awtCursorId: Int) =
        pointerHoverIcon(PointerIcon(Cursor(awtCursorId)))

    private fun resize(sides: Int) {
        val pointPos = MouseInfo.getPointerInfo().location
        val diffX = pointPos.x - initialPointPos.x
        val diffY = pointPos.y - initialPointPos.y
        var newXPos = window.x
        var newYPos = window.y
        var newWidth = window.width
        var newHeight = window.height

        if (contains(sides, Side.Left)) {
            newWidth = initialWindowSize.width - diffX
            newWidth = newWidth.coerceAtLeast(window.minimumSize.width)
            newXPos = initialWindowPos.x + initialWindowSize.width - newWidth
        } else if (contains(sides, Side.Right)) {
            newWidth = initialWindowSize.width + diffX
        }
        if (contains(sides, Side.Top)) {
            newHeight = initialWindowSize.height - diffY
            newHeight = newHeight.coerceAtLeast(window.minimumSize.height)
            newYPos = initialWindowPos.y + initialWindowSize.height - newHeight
        } else if (contains(sides, Side.Bottom)) {
            newHeight = initialWindowSize.height + diffY
        }
        window.setLocation(newXPos, newYPos)
        window.setSize(newWidth, newHeight)
    }

    private fun contains(value: Int, other: Int): Boolean {
        if (value and other == other) {
            return true
        }
        return false
    }

    private object Side {
        val Left = 0x0001
        val Top = 0x0010
        val Right = 0x0100
        val Bottom = 0x1000
    }
}