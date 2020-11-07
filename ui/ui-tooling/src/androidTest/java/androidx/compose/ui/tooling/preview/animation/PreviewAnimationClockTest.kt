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

import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TransitionAnimation
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(InternalAnimationApi::class)
class PreviewAnimationClockTest {

    private lateinit var testClock: TestPreviewAnimationClock

    @Before
    fun setUp() {
        testClock = TestPreviewAnimationClock()
    }

    @Test
    fun setClockTimeIsRelative() {
        val previewAnimationClock = PreviewAnimationClock(100)
        previewAnimationClock.setClockTime(300)
        assertEquals(400, previewAnimationClock.clock.clockTimeMillis)
    }

    @Test
    fun subscribeAndUnsubscribeTransitionAnimationShouldNotify() {
        val anim = TransitionAnimation(offsetDef, testClock)
        assertEquals(0, testClock.subscribeCount)
        assertEquals(0, testClock.unsubscribeCount)

        // Force subscription
        anim.toState(Offset.O2)
        assertEquals(1, testClock.subscribeCount)

        // Force unsubscription.
        anim.snapToState(Offset.O1)
        assertEquals(1, testClock.unsubscribeCount)

        val subscribedAnimation = testClock.subscribedAnimation
        assertEquals(subscribedAnimation, testClock.unsubscribedAnimation)

        // Check the animation is a non-monotonic TransitionAnimation
        assertEquals(ComposeAnimationType.TRANSITION_ANIMATION, subscribedAnimation.type)
        val animation = subscribedAnimation.animationObject as TransitionAnimation<*>
        assertEquals(anim, animation)
        assertFalse(animation.monotonic)
        val states = subscribedAnimation.states
        assertEquals(2, states.size)
        assertTrue(states.contains(Offset.O1))
        assertTrue(states.contains(Offset.O2))
    }

    @Test
    fun unsupportedObserverShouldNotNotify() {
        val observer = object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                // Do nothing
            }
        }

        assertEquals(0, testClock.subscribeCount)
        assertEquals(0, testClock.notifySubscribeCount)
        testClock.subscribe(observer)
        assertEquals(1, testClock.subscribeCount)
        assertEquals(0, testClock.notifySubscribeCount)

        assertEquals(0, testClock.unsubscribeCount)
        assertEquals(0, testClock.notifyUnsubscribeCount)
        testClock.unsubscribe(observer)
        assertEquals(1, testClock.unsubscribeCount)
        assertEquals(0, testClock.notifyUnsubscribeCount)
    }

    @Test
    fun disposeClearsCachedAnimations() {
        setUpOffsetScenario()

        assertFalse(testClock.observersToAnimations.isEmpty())
        assertFalse(testClock.seekableAnimations.isEmpty())

        testClock.dispose()

        assertTrue(testClock.observersToAnimations.isEmpty())
        assertTrue(testClock.seekableAnimations.isEmpty())
    }

    @Test
    fun updateSeekableAnimationModifiesCachedValue() {
        val seekableAnimation = testClock.seekableAnimations[setUpRotationColorScenario()]!!

        assertEquals(RotationColor.RC1, seekableAnimation.fromState)
        assertEquals(RotationColor.RC3, seekableAnimation.toState)
    }

    @Test
    fun updateAnimationStatesDoesNotResubscribe() {
        setUpRotationColorScenario()

        // The animation in the scenario should have been subscribed and properly notified.
        assertEquals(1, testClock.subscribeCount)
        assertEquals(1, testClock.notifySubscribeCount)

        testClock.updateAnimationStates()
        // subscribe is called when updating the states, but since it follows an unsubscription,
        // we skip the method behavior and, for instance, don't notify the subscription again.
        assertEquals(2, testClock.subscribeCount)
        assertEquals(1, testClock.notifySubscribeCount)
    }

    @Test
    fun updateAnimationStatesResetsClock() {
        setUpRotationColorScenario()
        testClock.setClockTime(123)
        assertEquals(123, testClock.clock.clockTimeMillis)

        testClock.updateAnimationStates()
        assertEquals(0, testClock.clock.clockTimeMillis)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun updateAnimationStatesUpdatesAllTransitionAnimations() {
        val rotationAnimation =
            setUpRotationColorScenario().animationObject as TransitionAnimation<RotationColor>
        val offsetAnimation = setUpOffsetScenario().animationObject as TransitionAnimation<Offset>

        testClock.updateAnimationStates()

        // offset animates from O1 (0) and rotation animates from RC1 (0)
        assertEquals(0f, offsetAnimation[offset], eps)
        assertEquals(0f, rotationAnimation[rotation], eps)
        // Animations take 800ms and 1000ms, so setting the clock to 1000 should make both finish
        testClock.setClockTime(1000)
        // offset animates to O2 (100) and rotation animates to RC3 (360)
        assertEquals(100f, offsetAnimation[offset], eps)
        assertEquals(360f, rotationAnimation[rotation], eps)
    }

    @Test
    fun getAnimatedPropertiesReturnsValuesAtCurrentTime() {
        val rotationAnimation = setUpRotationColorScenario()
        val offsetAnimation = setUpOffsetScenario()
        testClock.setClockTime(200)
        var animatedProperties = testClock.getAnimatedProperties(rotationAnimation)

        val color = animatedProperties.single { it.label == "borderColor" }
        // We're animating from RC1 (Red) to RC3 (Green). Since there is no transition defined
        // for the color property, we snap to the end state.
        assertEquals(Color.Green, color.value)

        val rotation = animatedProperties.single { it.label == "myRotation" }
        // We're animating from RC1 (0 degrees) to RC3 (360 degrees). There is a transition of
        // 1000ms defined for the rotation, and we set the clock to 20% of this time.
        assertEquals(72f, rotation.value as Float, eps)

        animatedProperties = testClock.getAnimatedProperties(offsetAnimation)
        val offset = animatedProperties.single()
        // We're animating from O1 (0) to O2 (100). There is a transition of 800ms defined for
        // the offset, and we set the clock to 25% of this time.
        assertEquals(25f, offset.value as Float, eps)
    }

    @Test
    fun maxDurationReturnsLongestDuration() {
        // When there are no animations, we should return an invalid duration.
        assertTrue(testClock.getMaxDuration() < 0)
        setUpRotationColorScenario() // 1000ms
        setUpOffsetScenario() // 800ms

        assertEquals(1000, testClock.getMaxDuration())
    }

    @Test
    fun maxDurationPerIterationReturnsLongestSingleIteration() {
        TransitionAnimation(repeatablesDef, testClock).toState("state2")
        val repeatableAnimation = testClock.observersToAnimations.values.single()
        testClock.updateSeekableAnimation(repeatableAnimation, "state1", "state2")
        assertEquals(300, testClock.getMaxDurationPerIteration()) // 300ms iteration
        assertEquals(1500, testClock.getMaxDuration()) // 5 iterations of 300ms

        setUpRotationColorScenario() // 1000ms
        // the rotation animation takes longer than a single iteration of the repeatable animation
        assertEquals(1000, testClock.getMaxDurationPerIteration())
        // total duration is still the same, as the repeatable animation will take longer in total
        assertEquals(1500, testClock.getMaxDuration())
    }

    @Test
    fun animationLabelIsSetExplicitlyOrImplicitly() {
        TransitionAnimation(rotationColorDef, testClock, label = "MyRot").toState(RotationColor.RC2)
        val rotationAnimation = testClock.observersToAnimations.values.single {
            it.states.contains(RotationColor.RC1)
        }
        // Label explicitly set
        assertEquals("MyRot", rotationAnimation.label)

        val offsetAnimation = setUpOffsetScenario()
        // Label is not explicitly set, but inferred from the state type
        assertEquals("Offset", offsetAnimation.label)
    }

    @Test
    fun callbackCalledWhenSettingClockTime() {
        var callbackCalledCount = 0
        val clock = TestPreviewAnimationClock { callbackCalledCount++ }
        clock.setClockTime(10)
        clock.setClockTime(20)

        assertEquals(2, callbackCalledCount)
    }

    // Sets up a transition animation scenario, going from RotationColor.RC1 to RotationColor.RC3.
    private fun setUpRotationColorScenario(): ComposeAnimation {
        TransitionAnimation(rotationColorDef, testClock).toState(RotationColor.RC2)
        val composeAnimation = testClock.observersToAnimations.values.single {
            it.states.contains(RotationColor.RC1)
        }
        testClock.updateSeekableAnimation(composeAnimation, RotationColor.RC1, RotationColor.RC3)
        return composeAnimation
    }

    // Sets up a transition animation scenario, going from from Offset.O1 to Offset.O2.
    private fun setUpOffsetScenario(): ComposeAnimation {
        TransitionAnimation(offsetDef, testClock).toState(Offset.O2)
        val composeAnimation = testClock.observersToAnimations.values.single {
            it.states.contains(Offset.O1)
        }
        testClock.updateSeekableAnimation(composeAnimation, Offset.O1, Offset.O2)
        return composeAnimation
    }

    private class TestPreviewAnimationClock(setClockTimeCallback: () -> Unit = {}) :
        PreviewAnimationClock(0, setClockTimeCallback) {
        lateinit var subscribedAnimation: ComposeAnimation
        lateinit var unsubscribedAnimation: ComposeAnimation
        var subscribeCount = 0
        var notifySubscribeCount = 0
        var unsubscribeCount = 0
        var notifyUnsubscribeCount = 0

        override fun notifySubscribe(animation: ComposeAnimation) {
            subscribedAnimation = animation
            notifySubscribeCount++
        }

        override fun notifyUnsubscribe(animation: ComposeAnimation) {
            unsubscribedAnimation = animation
            notifyUnsubscribeCount++
        }

        override fun subscribe(observer: AnimationClockObserver) {
            super.subscribe(observer)
            subscribeCount++
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            super.unsubscribe(observer)
            unsubscribeCount++
        }
    }
}

private enum class Offset { O1, O2 }

private enum class RotationColor { RC1, RC2, RC3 }

private const val eps = 0.00001f

private val rotation = FloatPropKey(label = "myRotation")
private val offset = FloatPropKey(label = "myOffset")
private val color = ColorPropKey(label = "borderColor")
private val floatProp = FloatPropKey()

private val repeatablesDef = transitionDefinition<String> {
    state("state1") {
        this[floatProp] = 0f
    }
    state("state2") {
        this[floatProp] = 0f
    }
    transition {
        floatProp using repeatable(
            iterations = 5,
            animation = tween(durationMillis = 300, easing = LinearEasing)
        )
    }
}

private val offsetDef = transitionDefinition<Offset> {
    state(Offset.O1) {
        this[offset] = 0f
    }
    state(Offset.O2) {
        this[offset] = 100f
    }

    transition {
        offset using tween(durationMillis = 800, easing = LinearEasing)
    }
}

private val rotationColorDef = transitionDefinition<RotationColor> {
    state(RotationColor.RC1) {
        this[rotation] = 0f
        this[color] = Color.Red
    }
    state(RotationColor.RC2) {
        this[rotation] = 180f
        this[color] = Color.Blue
    }

    state(RotationColor.RC3) {
        this[rotation] = 360f
        this[color] = Color.Green
    }

    transition {
        rotation using tween(durationMillis = 1000, easing = LinearEasing)
    }
}
