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

package androidx.compose.ui.test.junit4

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.android.AndroidTestAnimationClock
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to monitor and take over the animation clock in the composition. It substitutes
 * the ambient animation clock provided at the root of the composition tree with a
 * [TestAnimationClock].
 */
@ExperimentalTesting
internal class AndroidAnimationClockTestRule(
    private val composeIdlingResource: ComposeIdlingResource
) : AnimationClockTestRule {

    /** Backing property for [clock] */
    private val _clock = AndroidTestAnimationClock()
    override val clock: TestAnimationClock get() = _clock

    override fun apply(base: Statement, description: Description?): Statement {
        return AnimationClockStatement(base)
    }

    @OptIn(InternalAnimationApi::class)
    private inner class AnimationClockStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            val oldFactory = rootAnimationClockFactory
            composeIdlingResource.registerTestClock(clock)
            rootAnimationClockFactory = { clock }
            try {
                base.evaluate()
            } finally {
                try {
                    _clock.dispose()
                } finally {
                    rootAnimationClockFactory = oldFactory
                    composeIdlingResource.unregisterTestClock(clock)
                }
            }
        }
    }
}

@Deprecated(
    message = "AnimationClockTestRule is no longer supported as a standalone solution. Retrieve " +
        "it from your ComposeTestRule instead",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("composeTestRule.clockTestRule")
)
@ExperimentalTesting
@Suppress("DocumentExceptions")
actual fun createAnimationClockRule(): AnimationClockTestRule =
    throw UnsupportedOperationException()
