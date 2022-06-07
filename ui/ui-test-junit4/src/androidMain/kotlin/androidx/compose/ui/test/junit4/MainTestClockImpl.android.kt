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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.test.frameDelayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainTestClockImpl(
    testScheduler: TestCoroutineScheduler,
    frameClock: TestMonotonicFrameClock
) : AbstractMainTestClock(
    testScheduler,
    frameClock.frameDelayMillis,
    ::runOnUiThread
) {
    internal val hasAwaiters = frameClock.hasAwaiters
}