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
import androidx.compose.animation.core.MonotonicFrameAnimationClock
import androidx.compose.animation.core.rootAnimationClockFactory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class MonotonicFrameClockTestRule : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return AnimationClockStatement(base)
    }

    @OptIn(InternalAnimationApi::class)
    private inner class AnimationClockStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            val oldFactory = rootAnimationClockFactory
            rootAnimationClockFactory = { MonotonicFrameAnimationClock(it) }
            try {
                base.evaluate()
            } finally {
                rootAnimationClockFactory = oldFactory
            }
        }
    }
}
