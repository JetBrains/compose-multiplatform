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

import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.Utils.createTestAnimatedVisibility
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AnimatedVisibilityComposeAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun createComposeAnimation() {
        lateinit var composeAnimation: AnimatedVisibilityComposeAnimation

        rule.setContent {
            val animatedVisibility = createTestAnimatedVisibility()
            composeAnimation = animatedVisibility.parseAnimatedVisibility()
            Assert.assertEquals("TestAnimatedVisibility", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.ANIMATED_VISIBILITY, composeAnimation.type)
            Assert.assertEquals(animatedVisibility, composeAnimation.animationObject)
            Assert.assertNull(composeAnimation.childTransition)
        }
    }
}