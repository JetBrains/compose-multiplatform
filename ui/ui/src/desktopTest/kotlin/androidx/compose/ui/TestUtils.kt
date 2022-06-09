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

package androidx.compose.ui

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import java.awt.AWTEvent
import java.awt.Component
import java.awt.EventQueue
import java.awt.Image
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.InvocationEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Objects
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import org.jetbrains.skiko.MainUIDispatcher

fun testImage(color: Color): Painter = run {
    val bitmap = ImageBitmap(100, 100)
    val paint = Paint().apply { this.color = color }
    Canvas(bitmap).drawRect(0f, 0f, 100f, 100f, paint)
    BitmapPainter(bitmap)
}

fun Image.readFirstPixel(): Color {
    val image = (this as MultiResolutionImage).getResolutionVariant(1.0, 1.0) as BufferedImage
    return Color(image.getRGB(0, 0))
}

fun Icon.readFirstPixel() = (this as ImageIcon).image.readFirstPixel()

private val os = System.getProperty("os.name").lowercase()
internal val isLinux = os.startsWith("linux")
internal val isWindows = os.startsWith("win")
internal val isMacOs = os.startsWith("mac")

fun Window.sendKeyEvent(
    code: Int,
    modifiers: Int = 0
): Boolean {
    val event = KeyEvent(
        // if we would just use `focusOwner` then it will be null if the window is minimized
        mostRecentFocusOwner,
        KeyEvent.KEY_PRESSED,
        0,
        modifiers,
        code,
        code.toChar(),
        KeyEvent.KEY_LOCATION_STANDARD
    )
    dispatchEvent(event)
    return event.isConsumed
}

fun JFrame.sendMouseEvent(
    id: Int,
    x: Int,
    y: Int,
    modifiers: Int = 0
): Boolean {
    // we use width and height instead of x and y because we can send (-1, -1), but still need
    // the component inside window
    val component = findComponentAt(width / 2, height / 2)
    val event = MouseEvent(
        component,
        id,
        0,
        modifiers,
        x,
        y,
        1,
        false
    )
    component.dispatchEvent(event)
    return event.isConsumed
}

fun JFrame.sendMouseWheelEvent(
    x: Int,
    y: Int,
    scrollType: Int = MouseWheelEvent.WHEEL_UNIT_SCROLL,
    wheelRotation: Double = 0.0,
    modifiers: Int = 0,
): Boolean {
    // we use width and height instead of x and y because we can send (-1, -1), but still need
    // the component inside window
    val component = findComponentAt(width / 2, height / 2)
    val event = MouseWheelEvent(
        component,
        MouseWheelEvent.MOUSE_WHEEL,
        0,
        modifiers,
        x,
        y,
        x,
        y,
        1,
        false,
        scrollType,
        1,
        wheelRotation.toInt(),
        wheelRotation
    )
    component.dispatchEvent(event)
    return event.isConsumed
}

private val EventComponent = object : Component() {}

internal fun awtWheelEvent(isScrollByPages: Boolean = false) = MouseWheelEvent(
    EventComponent,
    MouseWheelEvent.MOUSE_WHEEL,
    0,
    0,
    0,
    0,
    0,
    false,
    if (isScrollByPages) {
        MouseWheelEvent.WHEEL_BLOCK_SCROLL
    } else {
        MouseWheelEvent.WHEEL_UNIT_SCROLL
    },
    1,
    0
)

fun Component.performClick() {
    dispatchEvent(MouseEvent(this, MouseEvent.MOUSE_PRESSED, 0, 0,1, 1, 1, false, MouseEvent.BUTTON1))
    dispatchEvent(MouseEvent(this, MouseEvent.MOUSE_RELEASED, 0, 0,1, 1, 1, false, MouseEvent.BUTTON1))
}

// TODO(demin): It seems this not-so-good synchronization
//  doesn't cause flakiness in our window tests.
//  But more robust solution will be to using something like UiUtil
/**
 * Wait until all scheduled tasks in Event Dispatch Thread are performed.
 * New scheduled tasks in these tasks also will be performed
 */
suspend fun awaitEDT() {
    // Most of the work usually is done after the first yield(), almost all of the work -
    // after fourth yield()
    repeat(100) {
        yield()
    }
}