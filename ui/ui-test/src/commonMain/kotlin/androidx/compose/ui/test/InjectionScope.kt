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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * The receiver scope of all input injection lambdas offered in `ui-test`, such as
 * [performTouchInput] and [performMouseInput].
 *
 * This scope offers several properties that allow you to get [coordinates][Offset] within the
 * node you're interacting on, like the [topLeft] corner, its [center], or some percentage of the
 * size ([percentOffset]).
 *
 * All positional properties are expressed in pixels. [InjectionScope] implements [Density] so
 * you can convert between px and dp as you wish. The density used is taken from the
 * [SemanticsNode][androidx.compose.ui.semantics.SemanticsNode] from the
 * [SemanticsNodeInteraction] on which the input injection method is called.
 */
@JvmDefaultWithCompatibility
interface InjectionScope : Density {
    /**
     * The default time between two successive events.
     */
    val eventPeriodMillis get() = InputDispatcher.eventPeriodMillis

    /**
     * Adds the given [durationMillis] to the current event time, delaying the next event by that
     * time.
     */
    fun advanceEventTime(durationMillis: Long = eventPeriodMillis)

    /**
     * The size of the visible part of the node we're interacting with in px, i.e. its clipped
     * bounds.
     */
    val visibleSize: IntSize

    /**
     * The [ViewConfiguration] in use by the
     * [SemanticsNode][androidx.compose.ui.semantics.SemanticsNode] from the
     * [SemanticsNodeInteraction] on which the input injection method is called.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * The width of the node in px. Shorthand for [visibleSize.width][visibleSize].
     */
    val width: Int get() = visibleSize.width

    /**
     * The height of the node in px. Shorthand for [visibleSize.height][visibleSize].
     */
    val height: Int get() = visibleSize.height

    /**
     * The x-coordinate for the left edge of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val left: Float get() = 0f

    /**
     * The y-coordinate for the bottom of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val top: Float get() = 0f

    /**
     * The x-coordinate for the center of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val centerX: Float get() = width / 2f

    /**
     * The y-coordinate for the center of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val centerY: Float get() = height / 2f

    /**
     * The x-coordinate for the right edge of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that, unless `width == 0`, `right != width`. In particular, `right == width - 1f`,
     * because pixels are 0-based. If `width == 0`, `right == 0` too.
     */
    val right: Float get() = width.let { if (it == 0) 0f else it - 1f }

    /**
     * The y-coordinate for the bottom of the node we're interacting with in px, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that, unless `height == 0`, `bottom != height`. In particular, `bottom == height - 1f`,
     * because pixels are 0-based. If `height == 0`, `bottom == 0` too.
     */
    val bottom: Float get() = height.let { if (it == 0) 0f else it - 1f }

    /**
     * The top left corner of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val topLeft: Offset get() = Offset(left, top)

    /**
     * The center of the top edge of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val topCenter: Offset get() = Offset(centerX, top)

    /**
     * The top right corner of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that `topRight.x != width`, see [right].
     */
    val topRight: Offset get() = Offset(right, top)

    /**
     * The center of the left edge of the node we're interacting with, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val centerLeft: Offset get() = Offset(left, centerY)

    /**
     * The center of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     */
    val center: Offset get() = Offset(centerX, centerY)

    /**
     * The center of the right edge of the node we're interacting with, in the
     * node's local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that `centerRight.x != width`, see [right].
     */
    val centerRight: Offset get() = Offset(right, centerY)

    /**
     * The bottom left corner of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that `bottomLeft.y != height`, see [bottom].
     */
    val bottomLeft: Offset get() = Offset(left, bottom)

    /**
     * The center of the bottom edge of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that `bottomCenter.y != height`, see [bottom].
     */
    val bottomCenter: Offset get() = Offset(centerX, bottom)

    /**
     * The bottom right corner of the node we're interacting with, in the node's
     * local coordinate system, where (0, 0) is the top left corner of the node.
     *
     * Note that `bottomRight.x != width` and `bottomRight.y != height`, see [right] and [bottom].
     */
    val bottomRight: Offset get() = Offset(right, bottom)

    /**
     * Creates an [Offset] relative to the size of the node we're interacting with. [x] and [y]
     * are fractions of the [width] and [height], between `-1` and `1`.
     *
     * Note that `percentOffset(1f, 1f) != bottomRight`, see [right] and [bottom].
     *
     * For example: `percentOffset(.5f, .5f)` is the same as the [center]; `centerLeft +
     * percentOffset(.1f, 0f)` is a point 10% inward from the middle of the left edge; and
     * `bottomRight - percentOffset(.2f, .1f)` is a point 20% to the left and 10% to the top of the
     * bottom right corner.
     */
    fun percentOffset(
        /*@FloatRange(from = -1.0, to = 1.0)*/
        x: Float = 0f,
        /*@FloatRange(from = -1.0, to = 1.0)*/
        y: Float = 0f
    ): Offset {
        return Offset(x * width, y * height)
    }
}
