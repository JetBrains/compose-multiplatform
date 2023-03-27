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

import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.clock.TransitionClockTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class TransitionComposeAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun parseIntComposeAnimation() {
        val targetState by mutableStateOf(1)
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            val composeAnimation = transition.parse()!!
            Assert.assertEquals("TestTransition", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, composeAnimation.type)
            Assert.assertEquals(transition, composeAnimation.animationObject)
            Assert.assertEquals(setOf(targetState), composeAnimation.states)
        }
    }

    @Test
    fun parseNullableIntComposeAnimation() {
        val targetState: MutableState<Int?> = mutableStateOf(1)
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            val composeAnimation = transition.parse()!!
            Assert.assertEquals("TestTransition", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, composeAnimation.type)
            Assert.assertEquals(transition, composeAnimation.animationObject)
            Assert.assertEquals(setOf(targetState), composeAnimation.states)
        }
    }

    @Test
    fun parseEnumComposeAnimation() {
        val targetState by mutableStateOf(TransitionClockTest.EnumState.One)
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            val composeAnimation = transition.parse()!!
            Assert.assertEquals("TestTransition", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, composeAnimation.type)
            Assert.assertEquals(transition, composeAnimation.animationObject)
            Assert.assertEquals(
                setOf(
                    TransitionClockTest.EnumState.One,
                    TransitionClockTest.EnumState.Two,
                    TransitionClockTest.EnumState.Three
                ),
                composeAnimation.states
            )
        }
    }

    @Test
    fun parseStringComposeAnimation() {
        val targetState by mutableStateOf("State")
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            val composeAnimation = transition.parse()!!
            Assert.assertEquals("TestTransition", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, composeAnimation.type)
            Assert.assertEquals(transition, composeAnimation.animationObject)
            Assert.assertEquals(setOf("State"), composeAnimation.states)
        }
    }

    @Test
    fun parseCustomComposeAnimation() {
        val targetState by mutableStateOf(TransitionClockTest.CustomState(0))
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            val composeAnimation = transition.parse()!!
            Assert.assertEquals("TestTransition", composeAnimation.label)
            Assert.assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, composeAnimation.type)
            Assert.assertEquals(transition, composeAnimation.animationObject)
            Assert.assertEquals(setOf(TransitionClockTest.CustomState(0)), composeAnimation.states)
        }
    }
}