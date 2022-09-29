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

package androidx.compose.ui.platform

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * Contains methods to standard constants used in the UI for timeouts, sizes, and distances.
 */
@JvmDefaultWithCompatibility
interface ViewConfiguration {
    /**
     * The duration before a press turns into a long press.
     */
    val longPressTimeoutMillis: Long

    /**
     * The duration between the first tap's up event and the second tap's down
     * event for an interaction to be considered a double-tap.
     */
    val doubleTapTimeoutMillis: Long

    /**
     * The minimum duration between the first tap's up event and the second tap's down event for
     * an interaction to be considered a double-tap.
     */
    val doubleTapMinTimeMillis: Long

    /**
     * Distance in pixels a touch can wander before we think the user is scrolling.
     */
    val touchSlop: Float

    /**
     * The minimum touch target size. If layout has reduced the pointer input bounds below this,
     * the touch target will be expanded evenly around the layout to ensure that it is at least
     * this big.
     */
    val minimumTouchTargetSize: DpSize
        get() = DpSize(48.dp, 48.dp)
}