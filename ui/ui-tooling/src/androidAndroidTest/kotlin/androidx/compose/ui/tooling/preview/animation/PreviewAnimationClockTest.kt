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

package androidx.compose.ui.tooling.preview.animation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@MediumTest
@OptIn(InternalAnimationApi::class)
class PreviewAnimationClockTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var testClock: TestPreviewAnimationClock

    @Before
    fun setUp() {
        testClock = TestPreviewAnimationClock()
    }

    @Test
    fun callbackCalledWhenSettingClockTime() {
        var callbackCalledCount = 0
        val clock = TestPreviewAnimationClock { callbackCalledCount++ }
        clock.setClockTime(10)
        clock.setClockTime(20)

        assertEquals(2, callbackCalledCount)
    }

    @Ignore("b/178910730")
    @Test
    fun getAnimatedPropertiesReturnsValuesAtCurrentTime() {
        var rotationAnimation: ComposeAnimation? = null
        var offsetAnimation: ComposeAnimation? = null

        composeRule.setContent {
            rotationAnimation = setUpRotationColorScenario()
            offsetAnimation = setUpOffsetScenario()
        }
        composeRule.waitForIdle()

        testClock.setClockTime(200)
        var animatedProperties = testClock.getAnimatedProperties(rotationAnimation!!)

        val rotation = animatedProperties.single { it.label == "myRotation" }
        // We're animating from RC1 (0 degrees) to RC3 (360 degrees). There is a transition of
        // 1000ms defined for the rotation, and we set the clock to 20% of this time.
        assertEquals(72f, rotation.value as Float, eps)

        animatedProperties = testClock.getAnimatedProperties(offsetAnimation!!)
        val offset = animatedProperties.single()
        // We're animating from O1 (0) to O2 (100). There is a transition of 800ms defined for
        // the offset, and we set the clock to 25% of this time.
        assertEquals(25f, offset.value as Float, eps)

        testClock.setClockTime(1000)
        animatedProperties = testClock.getAnimatedProperties(rotationAnimation!!)
        val color = animatedProperties.single { it.label == "borderColor" }
        // We're animating from RC1 (Red) to RC3 (Green), 1000ms being the animation duration.
        assertEquals(Color.Blue, color.value)
    }

    @Ignore("b/178910730")
    @Test
    fun maxDurationReturnsLongestDuration() {
        // When there are no animations, we should return an invalid duration.
        assertTrue(testClock.getMaxDuration() < 0)
        composeRule.setContent {
            setUpRotationColorScenario() // 1000ms
            setUpOffsetScenario() // 800ms
        }
        composeRule.waitForIdle()
        testClock.setClockTime(0L)

        assertEquals(1000, testClock.getMaxDuration())
    }

    @Test
    fun disposeShouldNotifyUnsubscribed() {
        composeRule.setContent {
            testClock.trackTransition(updateTransition(Any()))
            testClock.trackTransition(updateTransition(Any()))
        }
        composeRule.waitForIdle()

        assertEquals(2, testClock.notifySubscribeCount)
        assertEquals(0, testClock.notifyUnsubscribeCount)

        testClock.dispose() // dispose() should unsubscribe all tracked animations
        assertEquals(2, testClock.notifyUnsubscribeCount)
    }

    @Test
    fun trackTransitionShouldNotifySubscribed() {
        assertEquals(0, testClock.notifySubscribeCount)
        composeRule.setContent { setUpOffsetScenario() }
        composeRule.waitForIdle()

        assertEquals(1, testClock.notifySubscribeCount)
        val subscribedAnimation = testClock.subscribedAnimation

        // Check the animation is a transition animation
        assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, subscribedAnimation.type)
        val states = subscribedAnimation.states
        assertEquals(2, states.size)
        assertTrue(states.contains(Offset.O1))
        assertTrue(states.contains(Offset.O2))
    }

    @Test
    fun disposeClearsCachedAnimations() {
        composeRule.setContent { setUpOffsetScenario() }
        composeRule.waitForIdle()

        assertFalse(testClock.trackedTransitions.isEmpty())
        assertFalse(testClock.transitionStates.isEmpty())

        testClock.dispose()

        assertTrue(testClock.trackedTransitions.isEmpty())
        assertTrue(testClock.transitionStates.isEmpty())
    }

    @Test
    fun updateFromAndToStatesModifiesCachedTransitionStates() {
        var animation: ComposeAnimation? = null
        composeRule.setContent {
            animation = setUpRotationColorScenario()
        }
        composeRule.waitForIdle()

        val stateBeforeUpdate = testClock.transitionStates.values.single()
        assertEquals(RotationColor.RC1, stateBeforeUpdate.current)
        assertEquals(RotationColor.RC3, stateBeforeUpdate.target)

        testClock.updateFromAndToStates(animation!!, RotationColor.RC2, RotationColor.RC1)

        val stateAfterUpdate = testClock.transitionStates.values.single()
        assertEquals(RotationColor.RC2, stateAfterUpdate.current)
        assertEquals(RotationColor.RC1, stateAfterUpdate.target)
    }

    @Test
    fun animationLabelIsSetExplicitlyOrImplicitly() {
        val someState = Any()
        composeRule.setContent {
            val transition = updateTransition(someState, "My animation label")
            testClock.trackTransition(transition)

            setUpOffsetScenario()
        }
        composeRule.waitForIdle()
        val animationWithLabel = testClock.trackedTransitions.keys.single {
            it.states.contains(someState)
        }
        // Label explicitly set
        assertEquals("My animation label", animationWithLabel.label)

        val animationWithoutLabel = testClock.trackedTransitions.keys.single {
            it.states.contains(Offset.O1)
        }
        // Label is not explicitly set, but inferred from the state type
        assertEquals("Offset", animationWithoutLabel.label)
    }

    // Sets up a transition animation scenario, going from RotationColor.RC1 to RotationColor.RC3.
    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun setUpRotationColorScenario(): ComposeAnimation {
        val transition = updateTransition(RotationColor.RC1)
        transition.animateFloat(
            label = "myRotation",
            transitionSpec = {
                tween(durationMillis = 1000, easing = LinearEasing)
            }
        ) {
            when (it) {
                RotationColor.RC1 -> 0f
                RotationColor.RC2 -> 180f
                RotationColor.RC3 -> 360f
            }
        }
        transition.animateColor(
            label = "borderColor",
            transitionSpec = {
                tween(durationMillis = 1000, easing = LinearEasing)
            }
        ) {
            when (it) {
                RotationColor.RC1 -> Color.Red
                RotationColor.RC2 -> Color.Green
                RotationColor.RC3 -> Color.Blue
            }
        }

        testClock.trackTransition(transition as Transition<Any>)
        val animation = testClock.trackedTransitions.keys.single {
            it.states.contains(RotationColor.RC1)
        }
        testClock.updateFromAndToStates(animation, RotationColor.RC1, RotationColor.RC3)
        return animation
    }

    // Sets up a transition animation scenario, going from from Offset.O1 to Offset.O2.
    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun setUpOffsetScenario(): ComposeAnimation {
        val transition = updateTransition(Offset.O1)
        transition.animateFloat(
            label = "myOffset",
            transitionSpec = {
                tween(durationMillis = 800, easing = LinearEasing)
            }
        ) {
            when (it) {
                Offset.O1 -> 0f
                Offset.O2 -> 100f
            }
        }

        testClock.trackTransition(transition as Transition<Any>)
        val animation = testClock.trackedTransitions.keys.single { it.states.contains(Offset.O1) }
        testClock.updateFromAndToStates(animation, Offset.O1, Offset.O2)
        return animation
    }

    private class TestPreviewAnimationClock(setClockTimeCallback: () -> Unit = {}) :
        PreviewAnimationClock(setClockTimeCallback) {
        lateinit var subscribedAnimation: ComposeAnimation
        var notifySubscribeCount = 0
        var notifyUnsubscribeCount = 0

        override fun notifySubscribe(animation: ComposeAnimation) {
            subscribedAnimation = animation
            notifySubscribeCount++
        }

        override fun notifyUnsubscribe(animation: ComposeAnimation) {
            notifyUnsubscribeCount++
        }
    }
}

private enum class Offset { O1, O2 }

private enum class RotationColor { RC1, RC2, RC3 }

private const val eps = 0.00001f