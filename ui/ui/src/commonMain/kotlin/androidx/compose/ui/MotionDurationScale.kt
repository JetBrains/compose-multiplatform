/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.runtime.Stable
import kotlin.coroutines.CoroutineContext

/**
 * Provides a duration scale for motion such as animations. When the duration [scaleFactor] is 0,
 * the motion will end in the next frame callback. Otherwise, the duration [scaleFactor] will be
 * used as a multiplier to scale the duration of the motion. The larger the scale, the longer the
 * motion will take to finish, and therefore the slower it will be perceived.
 */
@Stable
interface MotionDurationScale : CoroutineContext.Element {
    /**
     * Defines the multiplier for the duration of the motion. This value should be non-negative.
     *
     * A [scaleFactor] of 1.0f would play the motion in real time. 0f would cause motion to
     * finish in the next frame callback. Larger [scaleFactor] will result
     * in longer durations for the motion/animation (i.e. slower animation). For example,
     * a [scaleFactor] of 10f would cause an animation with a duration of 100ms to finish in
     * 1000ms.
     */
    val scaleFactor: Float

    override val key: CoroutineContext.Key<*> get() = Key

    companion object Key : CoroutineContext.Key<MotionDurationScale>
}
