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

package androidx.compose.testutils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpSize

/**
 * Default values for [TestViewConfiguration]. This object exists so we can leverage the default
 * implementation of members from [ViewConfiguration].
 */
private val Default = object : ViewConfiguration {
    override val longPressTimeoutMillis: Long = 500L
    override val doubleTapTimeoutMillis: Long = 300L
    override val doubleTapMinTimeMillis: Long = 40L
    override val touchSlop: Float = 18f
}

/**
 * A [ViewConfiguration] that can be used for testing. The default values are representative for
 * Android devices, but can be set to any value desired for a test. See the `With*` functions for
 * shorthands that create a [TestViewConfiguration] and provide it as a [LocalViewConfiguration].
 *
 * @see WithLongPressTimeoutMillis
 * @see WithDoubleTapTimeoutMillis
 * @see WithDoubleTapMinTimeMillis
 * @see WithTouchSlop
 * @see WithMinimumTouchTargetSize
 */
class TestViewConfiguration(
    override val longPressTimeoutMillis: Long = Default.longPressTimeoutMillis,
    override val doubleTapTimeoutMillis: Long = Default.doubleTapTimeoutMillis,
    override val doubleTapMinTimeMillis: Long = Default.doubleTapMinTimeMillis,
    override val touchSlop: Float = Default.touchSlop,
    override val minimumTouchTargetSize: DpSize = Default.minimumTouchTargetSize
) : ViewConfiguration

@Composable
fun WithLongPressTimeoutMillis(longPressTimeoutMillis: Long, content: @Composable () -> Unit) {
    WithViewConfiguration(
        TestViewConfiguration(longPressTimeoutMillis = longPressTimeoutMillis),
        content = content
    )
}

@Composable
fun WithDoubleTapTimeoutMillis(doubleTapTimeoutMillis: Long, content: @Composable () -> Unit) {
    WithViewConfiguration(
        TestViewConfiguration(doubleTapTimeoutMillis = doubleTapTimeoutMillis),
        content = content
    )
}

@Composable
fun WithDoubleTapMinTimeMillis(doubleTapMinTimeMillis: Long, content: @Composable () -> Unit) {
    WithViewConfiguration(
        TestViewConfiguration(doubleTapMinTimeMillis = doubleTapMinTimeMillis),
        content = content
    )
}

@Composable
fun WithTouchSlop(touchSlop: Float, content: @Composable () -> Unit) {
    WithViewConfiguration(
        TestViewConfiguration(touchSlop = touchSlop),
        content = content
    )
}

@Composable
fun WithMinimumTouchTargetSize(minimumTouchTargetSize: DpSize, content: @Composable () -> Unit) {
    WithViewConfiguration(
        TestViewConfiguration(minimumTouchTargetSize = minimumTouchTargetSize),
        content = content
    )
}

@Composable
fun WithViewConfiguration(
    testViewConfiguration: TestViewConfiguration,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalViewConfiguration provides testViewConfiguration,
        content = content
    )
}
