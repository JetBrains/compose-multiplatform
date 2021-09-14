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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Before
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

    @Test
    fun getAnimatedPropertiesReturnsValuesAtCurrentTime() {
        var rotationAnimation: ComposeAnimation? = null
        var offsetAnimation: ComposeAnimation? = null
        var animatedVisibility: Transition<Any>? = null

        composeRule.setContent {
            rotationAnimation = setUpRotationColorScenario()
            offsetAnimation = setUpOffsetScenario()
            animatedVisibility = createAnimationVisibility(1000)
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibility!!)
        composeRule.waitForIdle()

        testClock.setClockTime(200)
        composeRule.waitForIdle()

        var animatedProperties = testClock.getAnimatedProperties(rotationAnimation!!)
        val rotation = animatedProperties.single { it.label == "myRotation" }
        // We're animating from RC1 (0 degrees) to RC3 (360 degrees). There is a transition of
        // 1000ms defined for the rotation, and we set the clock to 20% of this time.
        assertEquals(72f, rotation.value as Float, eps)

        animatedProperties = testClock.getAnimatedProperties(offsetAnimation!!)
        val offset = animatedProperties.single { it.label == "myOffset" }
        // We're animating from O1 (0) to O2 (100). There is a transition of 800ms defined for
        // the offset, and we set the clock to 25% of this time.
        assertEquals(25f, offset.value as Float, eps)

        val animatedVisibilityComposeAnimation = testClock.trackedAnimatedVisibility.single()
        animatedProperties = testClock.getAnimatedProperties(animatedVisibilityComposeAnimation)
        val scale = animatedProperties.single { it.label == "box scale" }
        // We're animating from invisible to visible, which means PreEnter (scale 0.5f) to
        // Visible (scale 1f). Animation duration is 1000ms, so the current clock time
        // corresponds to 20% of it.
        assertEquals(0.6f, scale.value as Float, 0.0001f)

        testClock.setClockTime(1000)
        animatedProperties = testClock.getAnimatedProperties(rotationAnimation!!)
        val color = animatedProperties.single { it.label == "borderColor" }
        // We're animating from RC1 (Red) to RC3 (Green), 1000ms being the animation duration.
        assertEquals(Color.Blue, color.value)

        animatedProperties = testClock.getAnimatedProperties(animatedVisibilityComposeAnimation)
        val alpha = animatedProperties.single { it.label == "Built-in alpha" }
        // We're animating from invisible (Built-in alpha 0f) to visible (Built-in alpha 1f),
        // 1000ms being the animation duration.
        assertEquals(1f, alpha.value)
    }

    @Test
    fun getAnimatedPropertiesWithNotSyncedTime() {
        var rotationAnimation: ComposeAnimation? = null
        var offsetAnimation: ComposeAnimation? = null
        var animatedVisibility: Transition<Any>? = null

        composeRule.setContent {
            rotationAnimation = setUpRotationColorScenario()
            offsetAnimation = setUpOffsetScenario()
            animatedVisibility = createAnimationVisibility(1000)
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibility!!)
        composeRule.waitForIdle()
        val animatedVisibilityComposeAnimation = testClock.trackedAnimatedVisibility.single()
        testClock.setClockTimes(
            mapOf(
                rotationAnimation!! to 500,
                offsetAnimation!! to 200,
                animatedVisibilityComposeAnimation to 800
            )
        )
        composeRule.waitForIdle()

        var animatedProperties = testClock.getAnimatedProperties(rotationAnimation!!)
        val rotation = animatedProperties.single { it.label == "myRotation" }
        // We're animating from RC1 (0 degrees) to RC3 (360 degrees). There is a transition of
        // 1000ms defined for the rotation, and we set the clock to 50% of this time.
        assertEquals(180f, rotation.value as Float, eps)

        animatedProperties = testClock.getAnimatedProperties(offsetAnimation!!)
        val offset = animatedProperties.single { it.label == "myOffset" }
        // We're animating from O1 (0) to O2 (100). There is a transition of 800ms defined for
        // the offset, and we set the clock to 25% of this time.
        assertEquals(25f, offset.value as Float, eps)

        animatedProperties = testClock.getAnimatedProperties(animatedVisibilityComposeAnimation)
        val scale = animatedProperties.single { it.label == "box scale" }
        // We're animating from invisible to visible, which means PreEnter (scale 0.5f) to
        // Visible (scale 1f). Animation duration is 1000ms, so the current clock time
        // corresponds to 80% of it.
        assertEquals(0.9f, scale.value as Float, 0.0001f)

        animatedProperties = testClock.getAnimatedProperties(animatedVisibilityComposeAnimation)
        val alpha = animatedProperties.single { it.label == "Built-in alpha" }
        // We're animating from invisible (Built-in alpha 0f) to visible (Built-in alpha 1f),
        // 1000ms being the animation duration, clock time corresponds to 80% of it.
        assertEquals(0.8f, alpha.value)
    }

    @Test
    fun getAnimatedPropertiesReturnsAllDescendantAnimations() {
        var transitionAnimation: ComposeAnimation? = null

        composeRule.setContent {
            transitionAnimation = setUpOffsetScenario()
        }
        composeRule.waitForIdle()

        val animatedProperties = testClock.getAnimatedProperties(transitionAnimation!!)
        // getAnimatedProperties should return all the transition animations as well as the
        // animations of all descendant transitions
        assertNotNull(animatedProperties.single { it.label == "myOffset" })
        assertNotNull(animatedProperties.single { it.label == "child1 scale" })
        assertNotNull(animatedProperties.single { it.label == "child2 color" })
        assertNotNull(animatedProperties.single { it.label == "grandchild" })
    }

    @Test
    fun getAnimatedPropertiesReturnsChildAnimations() {
        var animatedVisibility: ComposeAnimation? = null

        composeRule.setContent {
            testClock.trackTransition(createAnimationVisibility(1000))
            animatedVisibility = testClock.trackedTransitions.single()
            testClock.updateFromAndToStates(animatedVisibility!!, true, false)
        }
        composeRule.waitForIdle()

        testClock.setClockTime(500)
        composeRule.waitForIdle()

        val animatedProperties = testClock.getAnimatedProperties(animatedVisibility!!)
        // We're animating from invisible to visible, which means PreEnter (scale 0.5f) to
        // Visible (scale 1f). Animation duration is 1000ms, so we're at 50%.
        val scale = animatedProperties.single { it.label == "box scale" }
        assertEquals(0.75f, scale.value as Float, 0.0001f)
        // We're animating from invisible (Built-in alpha 0f) to visible (Built-in alpha 1f).
        // Animation duration is 1000ms, so we're at 50%.
        val alpha = animatedProperties.single { it.label == "Built-in alpha" }
        assertEquals(0.5f, alpha.value as Float, 0.0001f)
    }

    @Test
    fun onSeekCallbackCalledWhenTrackingAnimatedVisibility() {
        var animatedVisibility: Transition<Any>? = null
        var onSeekCalls = 0
        composeRule.setContent {
            animatedVisibility = createAnimationVisibility(1000)
        }

        composeRule.waitForIdle()
        assertEquals(0, onSeekCalls)
        testClock.trackAnimatedVisibility(animatedVisibility!!) { onSeekCalls++ }
        assertEquals(1, onSeekCalls)
    }

    @Test
    fun getTransitions() {
        var rotationAnimation: ComposeAnimation? = null
        var offsetAnimation: ComposeAnimation? = null
        var animatedVisibility: Transition<Any>? = null

        composeRule.setContent {
            rotationAnimation = setUpRotationColorScenario()
            offsetAnimation = setUpOffsetScenario()
            animatedVisibility = createAnimationVisibility(1000)
        }

        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibility!!)
        testClock.setClockTime(200)
        composeRule.waitForIdle()

        var transitions = testClock.getTransitions(rotationAnimation!!, 100)

        val rotation = transitions.single { it.label == "myRotation" }
        // We're animating from RC1 (0 degrees) to RC3 (360 degrees),
        // 1000ms being the animation duration.
        assertEquals("myRotation", rotation.label)
        assertEquals(0, rotation.startTimeMillis)
        assertEquals(1000, rotation.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", rotation.specType)
        assertArrayEquals(
            arrayOf(0L, 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L),
            rotation.values.keys.sorted().toTypedArray()
        )

        val color = transitions.single { it.label == "borderColor" }
        // We're animating from RC1 (Red) to RC3 (Green), 1000ms being the animation duration.
        assertEquals("borderColor", color.label)
        assertEquals(0, color.startTimeMillis)
        assertEquals(1000, color.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", color.specType)
        assertArrayEquals(
            arrayOf(0L, 100L, 200L, 300L, 400L, 500L, 600L, 700L, 800L, 900L, 1000L),
            color.values.keys.sorted().toTypedArray()
        )

        transitions = testClock.getTransitions(offsetAnimation!!, 200)
        val offset = transitions.single { it.label == "myOffset" }
        // We're animating from O1 (0) to O2 (100), 800ms being the animation duration.
        assertEquals("myOffset", offset.label)
        assertEquals(0, offset.startTimeMillis)
        assertEquals(800, offset.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", offset.specType)
        assertArrayEquals(
            arrayOf(0L, 200L, 400L, 600L, 800L),
            offset.values.keys.sorted().toTypedArray()
        )

        val grandChild = transitions.single { it.label == "grandchild" }
        // We're animating from O1 (1.dp) to O2 (9.dp), 900ms being the animation duration.
        assertEquals("grandchild", grandChild.label)
        assertEquals(0, grandChild.startTimeMillis)
        assertEquals(900, grandChild.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", grandChild.specType)
        assertArrayEquals(
            arrayOf(0L, 200L, 400L, 600L, 800L, 900L),
            grandChild.values.keys.sorted().toTypedArray()
        )

        val animatedVisibilityComposeAnimation = testClock.trackedAnimatedVisibility.single()
        transitions = testClock.getTransitions(animatedVisibilityComposeAnimation, 450)
        val scale = transitions.single() { it.label == "box scale" }
        // We're animating from invisible to visible, which means PreEnter (scale 0.5f) to
        // Visible (scale 1f). Animation duration is 1000ms, so we're at 50%.
        assertEquals("box scale", scale.label)
        assertEquals(0, scale.startTimeMillis)
        assertEquals(1000, scale.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", scale.specType)
        assertArrayEquals(
            arrayOf(0L, 450L, 900L, 1000L),
            scale.values.keys.sorted().toTypedArray()
        )

        val alpha = transitions.single() { it.label == "Built-in alpha" }
        // We're animating from invisible (Built-in alpha 0f) to visible (Built-in alpha 1f).
        // Animation duration is 1000ms, so we're at 50%.
        assertEquals("Built-in alpha", alpha.label)
        assertEquals(0, alpha.startTimeMillis)
        assertEquals(1000, alpha.endTimeMillis)
        assertEquals("androidx.compose.animation.core.TweenSpec", alpha.specType)
        assertArrayEquals(
            arrayOf(0L, 450L, 900L, 1000L),
            alpha.values.keys.sorted().toTypedArray()
        )
    }

    @Test
    fun maxDurationReturnsLongestDuration() {
        // When there are no animations, we should return an invalid duration.
        assertTrue(testClock.getMaxDuration() < 0)
        var animatedVisibility900: Transition<Any>? = null
        var animatedVisibility1200: Transition<Any>? = null
        composeRule.setContent {
            setUpRotationColorScenario() // 1000ms
            setUpOffsetScenario() // 800ms
            animatedVisibility900 = createAnimationVisibility(900)
            animatedVisibility1200 = createAnimationVisibility(1200)
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibility900!!)

        testClock.setClockTime(0L)
        composeRule.waitForIdle()

        assertEquals(1000, testClock.getMaxDuration())

        testClock.trackAnimatedVisibility(animatedVisibility1200!!)
        composeRule.waitForIdle()

        assertEquals(1200, testClock.getMaxDuration())
    }

    @Test
    fun disposeShouldNotifyUnsubscribed() {
        var animatedVisibilityTransition: Transition<Any>? = null
        composeRule.setContent {
            animatedVisibilityTransition = createAnimationVisibility()
            testClock.trackTransition(updateTransition(Any()))
            testClock.trackTransition(updateTransition(Any()))
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibilityTransition!!)

        assertEquals(3, testClock.notifySubscribeCount)
        assertEquals(0, testClock.notifyUnsubscribeCount)

        testClock.dispose() // dispose() should unsubscribe all tracked animations
        assertEquals(3, testClock.notifyUnsubscribeCount)
    }

    @Test
    fun trackTransitionShouldNotifySubscribed() {
        var animatedVisibilityTransition: Transition<Any>? = null
        assertEquals(0, testClock.notifySubscribeCount)
        composeRule.setContent {
            animatedVisibilityTransition = createAnimationVisibility()
            setUpOffsetScenario()
        }
        composeRule.waitForIdle()

        assertEquals(1, testClock.notifySubscribeCount)
        val subscribedAnimation = testClock.subscribedAnimation

        // Check the animation is a transition animation
        assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, subscribedAnimation.type)
        val states = subscribedAnimation.states
        assertEquals(2, states.size)
        assertTrue(states.contains(Offset.O1))
        assertTrue(states.contains(Offset.O2))

        testClock.trackAnimatedVisibility(animatedVisibilityTransition!!)
        assertEquals(2, testClock.notifySubscribeCount)
        val subscribedAnimation2 = testClock.subscribedAnimation

        // Check the animation is an AnimatedVisibility animation
        assertEquals(ComposeAnimationType.ANIMATED_VISIBILITY, subscribedAnimation2.type)
    }

    @Test
    fun disposeClearsCachedAnimations() {
        var animatedVisibilityTransition: Transition<Any>? = null
        composeRule.setContent {
            setUpOffsetScenario()
            animatedVisibilityTransition = createAnimationVisibility()
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibilityTransition!!)

        assertFalse(testClock.trackedTransitions.isEmpty())
        assertFalse(testClock.transitionStates.isEmpty())
        assertFalse(testClock.trackedAnimatedVisibility.isEmpty())
        assertFalse(testClock.animatedVisibilityStates.isEmpty())

        testClock.dispose()

        assertTrue(testClock.trackedTransitions.isEmpty())
        assertTrue(testClock.transitionStates.isEmpty())
        assertTrue(testClock.trackedAnimatedVisibility.isEmpty())
        assertTrue(testClock.animatedVisibilityStates.isEmpty())
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
    fun updateAnimatedVisibilityModifiesCachedState() {
        var animatedVisibilityTransition: Transition<Any>? = null
        composeRule.setContent {
            animatedVisibilityTransition = createAnimationVisibility(isEnter = true)
        }
        composeRule.waitForIdle()
        testClock.trackAnimatedVisibility(animatedVisibilityTransition!!)
        val animation = testClock.trackedAnimatedVisibility.first()
        assertEquals(AnimatedVisibilityState.Enter, testClock.getAnimatedVisibilityState(animation))

        testClock.updateAnimatedVisibilityState(animation, AnimatedVisibilityState.Exit)
        assertEquals(AnimatedVisibilityState.Exit, testClock.getAnimatedVisibilityState(animation))
    }

    @Test
    fun animationLabelIsSetExplicitlyOrImplicitly() {
        val someState = Any()
        var animatedVisibilityTransition: Transition<Any>? = null
        var animatedVisibilityTransitionExplicitLabel: Transition<Any>? = null
        composeRule.setContent {
            val transition = updateTransition(someState, "My animation label")
            testClock.trackTransition(transition)

            setUpOffsetScenario()
            animatedVisibilityTransition = createAnimationVisibility(isEnter = false)
            animatedVisibilityTransitionExplicitLabel =
                createAnimationVisibility(isEnter = true, label = "My AnimatedVisibility label")
        }
        composeRule.waitForIdle()
        val animationWithLabel = testClock.trackedTransitions.single {
            it.states.contains(someState)
        }
        // Label explicitly set
        assertEquals("My animation label", animationWithLabel.label)

        val animationWithoutLabel = testClock.trackedTransitions.single {
            it.states.contains(Offset.O1)
        }
        // Label is not explicitly set, but inferred from the state type
        assertEquals("Offset", animationWithoutLabel.label)

        testClock.trackAnimatedVisibility(animatedVisibilityTransition!!)
        testClock.trackAnimatedVisibility(animatedVisibilityTransitionExplicitLabel!!)

        val animatedVisibilityExplicitLabel = testClock.trackedAnimatedVisibility.single {
            testClock.getAnimatedVisibilityState(it) == AnimatedVisibilityState.Enter
        }
        // Label explicitly set
        assertEquals("My AnimatedVisibility label", animatedVisibilityExplicitLabel.label)

        val animatedVisibilityImplicitLabel = testClock.trackedAnimatedVisibility.single {
            testClock.getAnimatedVisibilityState(it) == AnimatedVisibilityState.Exit
        }
        // Label is not explicitly set, so we fall back to the default AnimatedVisibility label
        assertEquals("AnimatedVisibility", animatedVisibilityImplicitLabel.label)
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
        val animation = testClock.trackedTransitions.single {
            it.states.contains(RotationColor.RC1)
        }
        testClock.updateFromAndToStates(animation, RotationColor.RC1, RotationColor.RC3)
        return animation
    }

    // Sets up a transition animation scenario, going from from Offset.O1 to Offset.O2.
    // The main transition in this scenario also has 2 child animations. One of them has a child
    // animation of its own.
    @OptIn(ExperimentalTransitionApi::class)
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

        val child1 = transition.createChildTransition { it == Offset.O1 }
        child1.animateFloat(label = "child1 scale") { pressed ->
            if (pressed) 1f else 3f
        }

        child1.createChildTransition { it }
            .animateDp(label = "grandchild", transitionSpec = {
                tween(durationMillis = 900, easing = LinearEasing)
            }) { parentState ->
                if (parentState) 1.dp else 9.dp
            }

        transition.createChildTransition { it }
            .animateColor(label = "child2 color") { state ->
                if (state == Offset.O1) Color.Red else Color.Blue
            }

        testClock.trackTransition(transition as Transition<Any>)
        val animation = testClock.trackedTransitions.single { it.states.contains(Offset.O1) }
        testClock.updateFromAndToStates(animation, Offset.O1, Offset.O2)
        return animation
    }

    // Creates a fadeIn/Out AnimatedVisibility with a given duration and returns the parent
    // Transition. The fadeIn/Out animation has a built-in alpha animation and in addition to
    // that we add an extra float animation (box scale). The `isEnter` parameter determines whether
    // the animation is initially enter or exit.
    @OptIn(ExperimentalAnimationApi::class)
    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun createAnimationVisibility(
        duration: Int = 500,
        isEnter: Boolean = true,
        label: String? = null
    ): Transition<Any> {
        fun <T> linearTween() = tween<T>(duration, easing = LinearEasing)
        val parentAnimatedVisibility = updateTransition(!isEnter, label)
        parentAnimatedVisibility.AnimatedVisibility(
            { it },
            enter = fadeIn(animationSpec = linearTween()),
            exit = fadeOut(animationSpec = linearTween()),
        ) {
            val scale by transition.animateFloat(
                transitionSpec = { linearTween() },
                label = "box scale"
            ) { enterExitState ->
                when (enterExitState) {
                    EnterExitState.PreEnter -> 0.5f
                    EnterExitState.Visible -> 1.0f
                    EnterExitState.PostExit -> 0.5f
                }
            }
            Box(Modifier.size((100 * scale).dp))
        }
        return parentAnimatedVisibility as Transition<Any>
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