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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation.Companion.parse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class InfiniteTransitionComposeAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun apiAvailable() {
        assertTrue(InfiniteTransitionComposeAnimation.apiAvailable)
        rule.setContent {
            val composeAnimation = AnimationSearch.InfiniteTransitionSearchInfo(
                rememberInfiniteTransition(),
                ToolingState(0L)
            ).parse()
            assertNotNull(composeAnimation)
            composeAnimation!!
            assertNotNull(composeAnimation.animationObject)
            assertNotNull(composeAnimation.label)
            assertEquals(1, composeAnimation.states.size)
            assertEquals(ComposeAnimationType.INFINITE_TRANSITION, composeAnimation.type)
        }
    }

    @Test
    fun apiIsNotAvailable() {
        InfiniteTransitionComposeAnimation.testOverrideAvailability(false)
        assertFalse(InfiniteTransitionComposeAnimation.apiAvailable)
        rule.setContent {
            val composeAnimation = AnimationSearch.InfiniteTransitionSearchInfo(
                rememberInfiniteTransition(),
                ToolingState(0L)
            ).parse()
            assertNull(composeAnimation)
        }
        InfiniteTransitionComposeAnimation.testOverrideAvailability(true)
    }
}