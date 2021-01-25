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

import androidx.compose.ui.unit.dp

/**
 * Modeled after Android's ViewConfiguration:
 * https://github.com/android/platform_frameworks_base/blob/master/core/java/android/view/ViewConfiguration.java
 */

/**
 * The time that must elapse before a tap gesture sends onTapDown, if there's
 * any doubt that the gesture is a tap.
 */
const val PressTimeoutMillis: Long = 100L

/** The time before a long press gesture attempts to win. */
const val LongPressTimeoutMillis: Long = 500L

/**
 * The maximum time from the start of the first tap to the start of the second
 * tap in a double-tap gesture.
 */
// TODO(shepshapard): In Android, this is actually the time from the first's up event
// to the second's down event, according to the ViewConfiguration docs.
const val DoubleTapTimeoutMillis: Long = 300L

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
 * The absolute cumulative average change in distance of all pointers from the
 * average pointer over time that must be surpassed to indicate the user is trying to scale.
 *
 * For example, if [ScaleSlop] is 5 DP and 2 pointers were 1 DP away from each
 * other and now are 11.00001 DP away from each other, the gesture will be interpreted to include
 * scaling (both pointers are slightly more than 5 pixels away from the average of the pointers
 * than they were).
 */
val ScaleSlop = TouchSlop
