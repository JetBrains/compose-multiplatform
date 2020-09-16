/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.gesture

import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.milliseconds

/**
 * Modeled after Android's ViewConfiguration:
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/ViewConfiguration.java
 */

/**
 * The time that must elapse before a tap gesture sends onTapDown, if there's
 * any doubt that the gesture is a tap.
 */
val PressTimeout: Duration = 100.milliseconds

/**
 * Maximum length of time between a tap down and a tap up for the gesture to be
 * considered a tap. (Currently not honored by the TapGestureFilter.)
 */
// TODO(shepshapard): Remove this, or implement a hover-tap gesture filter which
// uses this.
val HoverTapTimeout: Duration = 150.milliseconds

/**
 * Maximum distance between the down and up pointers for a tap. (Currently not
 * honored by the [TapGestureFilter].
 */
// TODO(shepshapard): Remove this or implement it correctly.
val HoverTapSlop = 20.dp

/** The time before a long press gesture attempts to win. */
val LongPressTimeout: Duration = 500.milliseconds

/**
 * The maximum time from the start of the first tap to the start of the second
 * tap in a double-tap gesture.
 */
// TODO(shepshapard): In Android, this is actually the time from the first's up event
// to the second's down event, according to the ViewConfiguration docs.
val DoubleTapTimeout: Duration = 300.milliseconds

/**
 * The minimum time from the end of the first tap to the start of the second
 * tap in a double-tap gesture. (Currently not honored by the
 * DoubleTapGestureFilter.)
 */
// TODO(shepshapard): Either implement this or remove the constant.
val DoubleTapMinTime: Duration = 40.milliseconds

/**
 * The distance a touch has to travel for the framework to be confident that
 * the gesture is a scroll gesture, or, inversely, the maximum distance that a
 * touch can travel before the framework becomes confident that it is not a
 * tap.
 */
// This value was empirically derived. We started at 8.0 and increased it to
// 18.0 after getting complaints that it was too difficult to hit targets.
val TouchSlop = 18.dp

/**
 * The maximum distance that the first touch in a double-tap gesture can travel
 * before deciding that it is not part of a double-tap gesture.
 * DoubleTapGestureFilter also restricts the second touch to this distance.
 */
val DoubleTapTouchSlop = TouchSlop

/**
 * Distance between the initial position of the first touch and the start
 * position of a potential second touch for the second touch to be considered
 * the second touch of a double-tap gesture.
 */
val DoubleTapSlop = 100.dp

/**
 * The time for which zoom controls (e.g. in a map interface) are to be
 * displayed on the screen, from the moment they were last requested.
 */
val ZoomControlsTimeout: Duration = 3000.milliseconds

/**
 * The distance a touch has to travel for the framework to be confident that
 * the gesture is a paging gesture. (Currently not used, because paging uses a
 * regular drag gesture, which uses kTouchSlop.)
 */
// TODO(shepshapard): Create variants of HorizontalDragGestureFilter et al for
// paging, which use this constant.
val PagingTouchSlop = TouchSlop * 2.dp

/**
 * The distance a touch has to travel for the framework to be confident that
 * the gesture is a panning gesture.
 */
val PanSlop = TouchSlop * 2.dp

/**
 * The absolute cumulative average change in distance of all pointers from the
 * average pointer over time that must be surpassed to indicate the user is trying to scale.
 *
 * For example, if [ScaleSlop] is 5 DP and 2 pointers were 1 DP away from each
 * other and now are 11.00001 DP away from each other, the gesture will be interpreted to include
 * scaling (both pointers are slightly more than 5 pixels away from the average of the pointers
 * than they were).
 */
val ScaleSlop = TouchSlop

/**
 * The margin around a dialog, popup menu, or other window-like composable inside
 * which we do not consider a tap to dismiss the composable. (Not currently used.)
 */
// TODO(shepshapard): Make ModalBarrier support this.
val WindowTouchSlop = 16.dp

/**
 * The minimum velocity for a touch to consider that touch to trigger a fling
 * gesture.
 */
// TODO(shepshapard): Make sure nobody has their own version of this.
val MinFlingVelocity = 50.dp // Logical pixels / second

/** Drag gesture fling velocities are clipped to this value. */
// TODO(shepshapard): Make sure nobody has their own version of this.
val MaxFlingVelocity = 8000.dp // Logical pixels / second

/**
 * The maximum time from the start of the first tap to the start of the second
 * tap in a jump-tap gesture.
 */
// TODO(shepshapard): Implement jump-tap gestures.
val JumpTapTimeout: Duration = 500.milliseconds
