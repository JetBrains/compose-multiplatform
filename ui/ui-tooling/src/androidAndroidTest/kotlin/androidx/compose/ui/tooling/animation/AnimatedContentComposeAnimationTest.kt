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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.AnimatedContentComposeAnimation.Companion.parseAnimatedContent
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimatedContentComposeAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun parseAnimation() {
        assertTrue(AnimatedContentComposeAnimation.apiAvailable)
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) {
            AnimatedContent(targetState = 1.dp) { targetCount ->
                Text(text = "Count: $targetCount")
            }
        }
        assertEquals(1, search.animations.size)
        val composeAnimation = search.animations.first().parseAnimatedContent()!!
        composeAnimation.animationObject.let {
            assertNotNull(it)
            assertEquals(1.dp, it.currentState)
            assertEquals(1.dp, it.targetState)
        }
        assertEquals("AnimatedContent", composeAnimation.label)
        assertEquals(1, composeAnimation.states.size)
        assertEquals(1.dp, composeAnimation.states.first())
    }

    @Test
    fun parseIfApiIsNotAvailable() {
        AnimatedContentComposeAnimation.testOverrideAvailability(false)
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) {
            AnimatedContent(targetState = 1.dp) { targetCount ->
                Text(text = "Count: $targetCount")
            }
        }
        assertEquals(1, search.animations.size)
        assertNull(search.animations.first().parseAnimatedContent())
        AnimatedContentComposeAnimation.testOverrideAvailability(true)
    }

    @Test
    fun parseAnimationWithNullState() {
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) {
            AnimatedContent(targetState = null) { targetCount ->
                Text(text = "Count: $targetCount")
            }
        }
        assertEquals(1, search.animations.size)
        assertNull(search.animations.first().parseAnimatedContent())
    }
}