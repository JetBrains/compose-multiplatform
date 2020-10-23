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

package androidx.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.LongPressTimeout
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.down
import androidx.compose.ui.test.height
import androidx.compose.ui.test.localToGlobal
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.move
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.movePointerBy
import androidx.compose.ui.test.movePointerTo
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.up
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.milliseconds
import androidx.compose.ui.util.annotation.FloatRange

/** @Deprecated Moved to androidx.compose.ui.test */
typealias GestureScope = androidx.compose.ui.test.GestureScope

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.width: Int
    get() = width

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.height: Int
    get() = height

/** @Deprecated Moved to androidx.compose.ui.test */
@Suppress("unused")
inline val GestureScope.left: Float
    get() = 0f

/** @Deprecated Moved to androidx.compose.ui.test */
@Suppress("unused")
inline val GestureScope.top: Float
    get() = 0f

/** @Deprecated Moved to androidx.compose.ui.test */
inline val GestureScope.centerX: Float
    get() = width / 2f

/** @Deprecated Moved to androidx.compose.ui.test */
inline val GestureScope.centerY: Float
    get() = height / 2f

/** @Deprecated Moved to androidx.compose.ui.test */
inline val GestureScope.right: Float
    get() = width.let { if (it == 0) 0f else it - 1f }

/** @Deprecated Moved to androidx.compose.ui.test */
inline val GestureScope.bottom: Float
    get() = height.let { if (it == 0) 0f else it - 1f }

/** @Deprecated Moved to androidx.compose.ui.test */
@Suppress("unused")
val GestureScope.topLeft: Offset
    get() = Offset(left, top)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.topCenter: Offset
    get() = Offset(centerX, top)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.topRight: Offset
    get() = Offset(right, top)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.centerLeft: Offset
    get() = Offset(left, centerY)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.center: Offset
    get() = Offset(centerX, centerY)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.centerRight: Offset
    get() = Offset(right, centerY)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.bottomLeft: Offset
    get() = Offset(left, bottom)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.bottomCenter: Offset
    get() = Offset(centerX, bottom)

/** @Deprecated Moved to androidx.compose.ui.test */
val GestureScope.bottomRight: Offset
    get() = Offset(right, bottom)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.percentOffset(
    @FloatRange(from = -1.0, to = 1.0) x: Float = 0f,
    @FloatRange(from = -1.0, to = 1.0) y: Float = 0f
) = percentOffset(x, y)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.localToGlobal(position: Offset) = localToGlobal(position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.click(position: Offset = center) = click(position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.longClick(
    position: Offset = center,
    duration: Duration = LongPressTimeout + 100.milliseconds
) = longClick(position, duration)

private val doubleClickDelay = 145.milliseconds

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.doubleClick(
    position: Offset = center,
    delay: Duration = doubleClickDelay
) = doubleClick(position, delay)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipe(
    start: Offset,
    end: Offset,
    duration: Duration = 200.milliseconds
) = swipe(start, end, duration)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.pinch(
    start0: Offset,
    end0: Offset,
    start1: Offset,
    end1: Offset,
    duration: Duration = 400.milliseconds
) = pinch(start0, end0, start1, end1, duration)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipeWithVelocity(
    start: Offset,
    end: Offset,
    @FloatRange(from = 0.0, to = 3.4e38 /* POSITIVE_INFINITY */) endVelocity: Float,
    duration: Duration = 200.milliseconds
) = swipeWithVelocity(start, end, endVelocity, duration)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipeUp() = swipeUp()

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipeDown() = swipeDown()

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipeLeft() = swipeLeft()

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.swipeRight() = swipeRight()

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.down(pointerId: Int, position: Offset) = down(pointerId, position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.down(position: Offset) = down(position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.moveTo(pointerId: Int, position: Offset) = moveTo(pointerId, position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.moveTo(position: Offset) = moveTo(position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.movePointerTo(pointerId: Int, position: Offset) =
    movePointerTo(pointerId, position)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.moveBy(pointerId: Int, delta: Offset) = moveBy(pointerId, delta)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.moveBy(delta: Offset) = moveBy(delta)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.movePointerBy(pointerId: Int, delta: Offset) =
    movePointerBy(pointerId, delta)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.move() = move()

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.up(pointerId: Int = 0) = up(pointerId)

/** @Deprecated Moved to androidx.compose.ui.test */
fun GestureScope.cancel() = cancel()