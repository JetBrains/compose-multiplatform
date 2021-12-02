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

import androidx.compose.ui.awt.ComposeLayer
import java.awt.Dimension
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

internal const val DefaultBorderThickness = 8

internal class UndecoratedWindowResizer(
    private val window: Window,
    layer: ComposeLayer,
    var enabled: Boolean = false,
    var borderThickness: Int = DefaultBorderThickness
) {
    private var initialPointPos = Point()
    private var initialWindowPos = Point()
    private var initialWindowSize = Dimension()
    private var sides = 0
    private var isResizing = false

    private val motionListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) = resize()
        override fun mouseMoved(event: MouseEvent) = changeCursor(event)
    }

    private val mouseListener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (sides != 0) {
                isResizing = true
            }
            initialPointPos = MouseInfo.getPointerInfo().location
            initialWindowPos = Point(window.x, window.y)
            initialWindowSize = Dimension(window.width, window.height)
        }
        override fun mouseReleased(event: MouseEvent) {
            isResizing = false
        }
    }

    init {
        layer.component.addMouseListener(mouseListener)
        layer.component.addMouseMotionListener(motionListener)
    }

    private fun changeCursor(event: MouseEvent) {
        if (!enabled || isResizing) {
            return
        }
        val point = event.getPoint()
        sides = getSides(point)
        when (sides) {
            Side.Left.value -> window.setCursor(Cursor(Cursor.W_RESIZE_CURSOR))
            Side.Top.value -> window.setCursor(Cursor(Cursor.N_RESIZE_CURSOR))
            Side.Right.value -> window.setCursor(Cursor(Cursor.E_RESIZE_CURSOR))
            Side.Bottom.value -> window.setCursor(Cursor(Cursor.S_RESIZE_CURSOR))
            Corner.LeftTop.value -> window.setCursor(Cursor(Cursor.NW_RESIZE_CURSOR))
            Corner.LeftBottom.value -> window.setCursor(Cursor(Cursor.SW_RESIZE_CURSOR))
            Corner.RightTop.value -> window.setCursor(Cursor(Cursor.NE_RESIZE_CURSOR))
            Corner.RightBottom.value -> window.setCursor(Cursor(Cursor.SE_RESIZE_CURSOR))
            else -> window.setCursor(Cursor(Cursor.DEFAULT_CURSOR))
        }
    }

    private fun getSides(point: Point): Int {
        var sides = 0
        val tolerance = borderThickness
        if (point.x <= tolerance) {
            sides += Side.Left.value
        }
        if (point.x >= window.width - tolerance) {
            sides += Side.Right.value
        }
        if (point.y <= tolerance) {
            sides += Side.Top.value
        }
        if (point.y >= window.height - tolerance) {
            sides += Side.Bottom.value
        }
        return sides
    }

    private fun resize() {
        if (!enabled || sides == 0) {
            return
        }

        val pointPos = MouseInfo.getPointerInfo().location
        val diffX = pointPos.x - initialPointPos.x
        val diffY = pointPos.y - initialPointPos.y
        var newXPos = window.x
        var newYPos = window.y
        var newWidth = window.width
        var newHeight = window.height

        if (contains(sides, Side.Left.value)) {
            newWidth = initialWindowSize.width - diffX
            newWidth = newWidth.coerceAtLeast(window.minimumSize.width)
            newXPos = initialWindowPos.x + initialWindowSize.width - newWidth
        } else if (contains(sides, Side.Right.value)) {
            newWidth = initialWindowSize.width + diffX
        }
        if (contains(sides, Side.Top.value)) {
            newHeight = initialWindowSize.height - diffY
            newHeight = newHeight.coerceAtLeast(window.minimumSize.height)
            newYPos = initialWindowPos.y + initialWindowSize.height - newHeight
        } else if (contains(sides, Side.Bottom.value)) {
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

    private enum class Side(val value: Int) {
        Left(0x0001),
        Top(0x0010),
        Right(0x0100),
        Bottom(0x1000)
    }

    private enum class Corner(val value: Int) {
        LeftTop(0x0011),
        LeftBottom(0x1001),
        RightTop(0x0110),
        RightBottom(0x1100)
    }
}