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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.AnimatedContentComposeAnimation.Companion.parseAnimatedContent
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.TransitionComposeAnimation
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.tooling.animation.parse
import androidx.compose.ui.tooling.animation.states.ComposeAnimationState
import androidx.compose.ui.tooling.animation.states.TargetState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalAnimationApi::class)
class TransitionClockTest {

    @get:Rule
    val rule = createComposeRule()

    enum class EnumState { One, Two, Three }

    data class CustomState(val number: Int)

    //region updateTransition() animations

    @Test
    fun clockWithEnumState() {
        val clock = createEnumTransitionClock()
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(EnumState.One, clock.state.initial)
            assertEquals(EnumState.One, clock.state.target)
            assertEquals(0, clock.getMaxDuration())
            assertEquals(0, clock.getMaxDurationPerIteration())
            val transitions = clock.getTransitions(500L)
            assertEquals(2, transitions.size)
            transitions[0].let {
                assertEquals("Animated Dp", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(0, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(10.dp, it.values[0])
            }
            transitions[1].let {
                assertEquals("Animated Color", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(0, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertTrue(it.values.containsKey(0))
                assertEquals(Color.Red, it.values[0])
            }
            // Change start and end state.
            clock.state = TargetState(EnumState.Three, EnumState.Two)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(EnumState.Three, clock.state.initial)
            assertEquals(EnumState.Two, clock.state.target)
            assertEquals(2000, clock.getMaxDuration())
            assertEquals(2000, clock.getMaxDurationPerIteration())
            val transitions = clock.getTransitions(500L)
            assertEquals(2, transitions.size)
            transitions[0].let {
                assertEquals("Animated Dp", it.label)
                assertEquals(100, it.startTimeMillis)
                assertEquals(1100, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.TweenSpec", it.specType)
                assertEquals(3, it.values.size)
                assertTrue(it.values.containsKey(100))
                assertTrue(it.values.containsKey(600))
                assertTrue(it.values.containsKey(1100))
                assertEquals(30.dp, it.values[100])
                assertEquals(20.dp, it.values[1100])
            }
            transitions[1].let {
                assertEquals("Animated Color", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(2000, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.TweenSpec", it.specType)
                assertEquals(5, it.values.size)
                assertTrue(it.values.containsKey(0))
                assertTrue(it.values.containsKey(500))
                assertTrue(it.values.containsKey(1000))
                assertTrue(it.values.containsKey(1500))
                assertTrue(it.values.containsKey(2000))
                assertEquals(Color.Green, it.values[0])
                assertEquals(Color.Gray, it.values[2000])
            }
        }
    }

    @Test
    fun clockWithAnimatedVisibility() {
        val clock = createBooleanTransitionClockWithAnimatedVisibility()
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(true, clock.state.initial)
            assertEquals(true, clock.state.target)
            assertEquals(0, clock.getMaxDuration())
            assertEquals(0, clock.getMaxDurationPerIteration())
            val transitions = clock.getTransitions(100L)
            assertEquals(0, transitions.size)
            // Change start and end state.
            clock.state = TargetState(initial = true, target = false)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(true, clock.state.initial)
            assertEquals(false, clock.state.target)
            assertEquals(350, clock.getMaxDuration(), 30)
            assertEquals(350, clock.getMaxDurationPerIteration(), 30)
            val transitions = clock.getTransitions(100L)
            assertEquals(3, transitions.size)

            transitions[0].let {
                assertEquals("Built-in alpha", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(330, it.endTimeMillis, 30)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertGreaterThanOrEqualTo(4, it.values.size)
            }
            transitions[1].let {
                assertEquals("Built-in shrink/expand", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(350, it.endTimeMillis, 30)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertGreaterThanOrEqualTo(4, it.values.size)
            }
            transitions[2].let {
                assertEquals("Built-in InterruptionHandlingOffset", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(0, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.SpringSpec", it.specType)
                assertEquals(1, it.values.size)
            }
        }
    }

    @Test
    fun clockWithCrossFade() {
        val clock = createBooleanTransitionClockWithCrossfade()
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(true, clock.state.initial)
            assertEquals(true, clock.state.target)
            assertEquals(0, clock.getMaxDuration())
            assertEquals(0, clock.getMaxDurationPerIteration())
            val transitions = clock.getTransitions(100L)
            assertEquals(1, transitions.size)
            // Change start and end state.
            clock.state = TargetState(initial = false, target = true)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(false, clock.state.initial)
            assertEquals(true, clock.state.target)
            assertEquals(300, clock.getMaxDuration(), 30)
            assertEquals(300, clock.getMaxDurationPerIteration(), 30)
            val transitions = clock.getTransitions(100L)
            assertEquals(1, transitions.size)

            transitions[0].let {
                assertEquals("FloatAnimation", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(300, it.endTimeMillis, 30)
                assertEquals("androidx.compose.animation.core.TweenSpec", it.specType)
                assertGreaterThanOrEqualTo(3, it.values.size)
            }
            transitions[0].values.let {
                assertTrue(it.containsKey(0L))
                assertTrue(it.containsKey(100L))
                assertTrue(it.containsKey(200L))
                assertTrue(it.containsKey(300L))
            }
        }
    }

    @Test
    fun clockWithAnimatedContent() {
        val clock = createBooleanTransitionClockWithAnimatedContent()
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(true, clock.state.initial)
            assertEquals(true, clock.state.target)
            assertEquals(0, clock.getMaxDuration())
            assertEquals(0, clock.getMaxDurationPerIteration())
            assertEquals(0, clock.getTransitions(100L).size)
            // Change start and end state.
            clock.state = TargetState(initial = false, target = true)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(false, clock.state.initial)
            assertEquals(true, clock.state.target)
            assertEquals(310, clock.getMaxDuration(), 30)
            assertEquals(310, clock.getMaxDurationPerIteration(), 30)
            val transitions = clock.getTransitions(100L)
            assertTrue(transitions.isNotEmpty())
            transitions.forEach { info ->
                assertTrue(info.startTimeMillis >= 0)
                assertTrue(info.endTimeMillis >= 0)
                assertTrue(info.values.isNotEmpty())
                assertNotNull(info.specType)
                assertNotNull(info.label)
            }
        }
    }

    @Test
    fun changeTimeForEnumClock() {
        changeTimeForClock(
            createEnumTransitionClock(),
            TargetState(EnumState.Three, EnumState.Two)
        )
    }

    @Test
    fun changeTimeForNullableEnumClock() {
        changeTimeForClock(
            createNullableEnumTransitionClock(),
            TargetState(EnumState.Three, EnumState.Two)
        )
    }

    @Test
    fun changeTimeForIntClock() {
        changeTimeForClock(
            createIntTransitionClock(),
            TargetState(3, 2)
        )
    }

    @Test
    fun changeTimeForCustomStateClock() {
        changeTimeForClock(
            createCustomStateTransitionClock(),
            TargetState(CustomState(3), CustomState(2))
        )
    }

    @Test
    fun incorrectStateIsAllowed() {
        val clock: ComposeAnimationClock<ComposeAnimation, ComposeAnimationState> =
            createIntTransitionClock()
                as ComposeAnimationClock<ComposeAnimation, ComposeAnimationState>
        assertEquals(TargetState(1, 1), clock.state)
        clock.state = TargetState("from", "to")
        // If type is erased it will actually allow to set incorrect type.
        assertEquals(TargetState("from", "to"), clock.state)
    }

    private fun <T> changeTimeForClock(clock: TransitionClock<T>, state: TargetState<T>) {
        rule.waitForIdle()
        rule.runOnIdle {
            // Default state
            assertEquals(
                listOf(
                    ComposeAnimatedProperty("Animated Dp", 10.dp),
                    ComposeAnimatedProperty("Animated Color", Color.Red)
                ), clock.getAnimatedProperties()
            )
            // Change start and end state.
            clock.state = state
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(
                listOf(
                    ComposeAnimatedProperty("Animated Dp", 30.dp),
                    ComposeAnimatedProperty("Animated Color", Color.Green)
                ), clock.getAnimatedProperties()
            )
            // Update clock time - set it to end of dp animation.
            clock.setClockTime(millisToNanos(1100L))
        }
        rule.waitForIdle()
        rule.runOnIdle {
            clock.getAnimatedProperties().let {
                assertEquals(2, it.size)
                assertEquals(ComposeAnimatedProperty("Animated Dp", 20.dp), it[0])
            }
            // Update clock time - set it to middle of dp animation.
            clock.setClockTime(millisToNanos(600L))
        }
        rule.waitForIdle()
        rule.runOnIdle {
            clock.getAnimatedProperties().let {
                assertEquals(2, it.size)
                assertEquals(22f, (it[0].value as Dp).value, 1f)
            }
            // Update clock time - set it to end of color animation.
            clock.setClockTime(millisToNanos(2000L))
        }
        rule.waitForIdle()
        rule.runOnIdle {
            clock.getAnimatedProperties().let {
                assertEquals(2, it.size)
                assertEquals(ComposeAnimatedProperty("Animated Color", Color.Gray), it[1])
            }
        }
    }

    private fun createEnumTransitionClock(): TransitionClock<EnumState> {
        return createTransitionClock(EnumState.One, {
            when (it) {
                EnumState.One -> 10.dp
                EnumState.Two -> 20.dp
                EnumState.Three -> 30.dp
            }
        }, {
            when (it) {
                EnumState.One -> Color.Red
                EnumState.Two -> Color.Gray
                EnumState.Three -> Color.Green
            }
        })
    }

    private fun createIntTransitionClock(): TransitionClock<Int> {
        return createTransitionClock(1, {
            when (it) {
                1 -> 10.dp
                2 -> 20.dp
                else -> 30.dp
            }
        }, {
            when (it) {
                1 -> Color.Red
                2 -> Color.Gray
                else -> Color.Green
            }
        })
    }

    private fun createCustomStateTransitionClock(): TransitionClock<CustomState> {
        return createTransitionClock(CustomState(1), {
            when (it) {
                CustomState(1) -> 10.dp
                CustomState(2) -> 20.dp
                else -> 30.dp
            }
        }, {
            when (it) {
                CustomState(1) -> Color.Red
                CustomState(2) -> Color.Gray
                else -> Color.Green
            }
        })
    }

    private fun createNullableEnumTransitionClock(): TransitionClock<EnumState?> {
        // It might not make sense, but it's allowed and it should not fail.
        return createTransitionClock(EnumState.One, {
            when (it) {
                EnumState.One -> 10.dp
                EnumState.Two -> 20.dp
                EnumState.Three -> 30.dp
                null -> 30.dp
            }
        }, {
            when (it) {
                EnumState.One -> Color.Red
                EnumState.Two -> Color.Gray
                EnumState.Three -> Color.Green
                null -> Color.Green
            }
        })
    }

    private fun <S> createTransitionClock(
        target: S,
        targetDpByState: @Composable (state: S) -> Dp,
        targetColorByState: @Composable (state: S) -> Color
    ): TransitionClock<S> {
        val targetState by mutableStateOf(target)
        lateinit var clock: TransitionClock<S>
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            transition.animateDp(
                transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
                label = "Animated Dp"
            ) { targetDpByState(it) }

            transition.animateColor(
                transitionSpec = { tween(durationMillis = 2000) },
                label = "Animated Color"
            ) { targetColorByState(it) }
            clock = TransitionClock(transition.parse() as TransitionComposeAnimation<S>)
        }
        return clock
    }

    private fun createBooleanTransitionClockWithAnimatedVisibility(): TransitionClock<Boolean> {
        val targetState by mutableStateOf(true)
        lateinit var clock: TransitionClock<Boolean>
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            transition.AnimatedVisibility(visible = { it }) {
                Text(text = "TestText")
            }
            clock = TransitionClock(transition.parse() as TransitionComposeAnimation<Boolean>)
        }
        return clock
    }

    private fun createBooleanTransitionClockWithAnimatedContent(): TransitionClock<Boolean> {
        val targetState by mutableStateOf(true)
        lateinit var clock: TransitionClock<Boolean>
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            transition.AnimatedContent {
                if (it) {
                    Text("TestText true")
                } else {
                    Text("TestText false")
                }
            }
            clock = TransitionClock(transition.parse() as TransitionComposeAnimation<Boolean>)
        }
        return clock
    }

    private fun createBooleanTransitionClockWithCrossfade(): TransitionClock<Boolean> {
        val targetState by mutableStateOf(true)
        lateinit var clock: TransitionClock<Boolean>
        rule.setContent {
            val transition = updateTransition(targetState, label = "TestTransition")
            transition.Crossfade {
                if (it) {
                    Text("TestText true")
                } else {
                    Text("TestText false")
                }
            }
            clock = TransitionClock(transition.parse() as TransitionComposeAnimation<Boolean>)
        }
        return clock
    }

    @Test
    fun childTransition() {
        val search = AnimationSearch.TransitionSearch { }
        rule.searchForAnimation(search) { childTransitions() }
        val clock = TransitionClock(search.animations.first().parse()!!)

        rule.runOnIdle {
            clock.getTransitions(100).let {
                assertEquals(5, it.size)
                assertEquals("Parent", it[0].label)
                assertEquals("Child1", it[1].label)
                assertEquals("Grandchild", it[2].label)
                assertEquals("GrandGrandchild", it[3].label)
                assertEquals("Child2", it[4].label)
            }
            clock.getAnimatedProperties().let {
                assertEquals(5, it.size)
                assertEquals("Parent", it[0].label)
                assertEquals("Child1", it[1].label)
                assertEquals("Grandchild", it[2].label)
                assertEquals("GrandGrandchild", it[3].label)
                assertEquals("Child2", it[4].label)
            }
        }
    }

    @OptIn(ExperimentalTransitionApi::class)
    @Composable
    fun childTransitions() {
        val state by remember { mutableStateOf(EnumState.One) }
        val parentTransition = updateTransition(state, label = "parent")
        parentTransition.animateDp(
            transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
            label = "Parent"
        ) { 10.dp }

        val child = parentTransition.createChildTransition(label = "child1") { it }.apply {
            this.animateDp(
                transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
                label = "Child1"
            ) { 10.dp }
        }
        val grandchild = child.createChildTransition(label = "child1") { it }.apply {
            this.animateDp(
                transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
                label = "Grandchild"
            ) { 10.dp }
        }
        grandchild.createChildTransition(label = "child1") { it }.apply {
            this.animateDp(
                transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
                label = "GrandGrandchild"
            ) { 10.dp }
        }
        parentTransition.createChildTransition(label = "child2") { it }.apply {
            this.animateDp(
                transitionSpec = { tween(durationMillis = 1000, delayMillis = 100) },
                label = "Child2"
            ) { 10.dp }
        }
    }

    //endregion

    //region AnimatedContent() animations
    @Test
    fun animatedContentClockState() {
        val search = AnimationSearch.AnimatedContentSearch { }
        val target = mutableStateOf<Dp?>(null)
        rule.searchForAnimation(search) { AnimatedContent(1.dp) { target.value = it } }
        val clock = TransitionClock(search.animations.first().parseAnimatedContent()!!)
        rule.runOnIdle {
            clock.setStateParameters(10.dp, 10.dp)
            clock.setClockTime(0)
        }
        rule.runOnIdle {
            assertEquals(TargetState(10.dp, 10.dp), clock.state)
            assertEquals(10.dp, target.value)
            // Change state
            clock.setStateParameters(20.dp, 40.dp)
            clock.setClockTime(0)
        }
        rule.runOnIdle {
            assertEquals(TargetState(20.dp, 40.dp), clock.state)
            assertEquals(40.dp, target.value)
        }
    }

    @Test
    fun animatedContentClockStateAsList() {
        val search = AnimationSearch.AnimatedContentSearch { }
        val target = mutableStateOf<IntSize?>(null)
        rule.searchForAnimation(search) { AnimatedContent(IntSize(10, 10)) { target.value = it } }
        val clock = TransitionClock(search.animations.first().parseAnimatedContent()!!)
        rule.runOnIdle {
            clock.setStateParameters(listOf(20, 30), listOf(40, 50))
            clock.setClockTime(0)
        }
        rule.runOnIdle {
            assertEquals(TargetState(IntSize(20, 30), IntSize(40, 50)), clock.state)
            assertEquals(IntSize(40, 50), target.value)
        }
    }

    @Test
    fun animatedContentClockProperties() {
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) { AnimatedContent(1.dp) {} }
        val clock = TransitionClock(search.animations.first().parseAnimatedContent()!!)
        rule.runOnIdle {
            clock.setStateParameters(10.dp, 10.dp)
        }
        rule.runOnIdle {
            assertEquals(2, clock.getAnimatedProperties().size)
            clock.setStateParameters(20.dp, 40.dp)
        }
        rule.runOnIdle {
            assertTrue(clock.getAnimatedProperties().isNotEmpty())
        }
    }

    @Test
    fun animatedContentClockTransitions() {
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) { AnimatedContent(1.dp) {} }
        val clock = TransitionClock(search.animations.first().parseAnimatedContent()!!)
        rule.runOnIdle {
            clock.setStateParameters(10.dp, 10.dp)
            clock.setClockTime(0)
        }
        rule.runOnIdle {
            // Default clock state.
            clock.getTransitions(100).let {
                assertEquals(2, it.size)
                it[0].let { info ->
                    assertEquals(0, info.startTimeMillis)
                    assertEquals(0, info.endTimeMillis)
                    assertEquals(1, info.values.size)
                    assertNotNull(info.specType)
                }
                it[1].let { info ->
                    assertEquals(0, info.startTimeMillis)
                    assertEquals(0, info.endTimeMillis)
                    assertEquals(1, info.values.size)
                    assertNotNull(info.specType)
                }
            }
            // Change state
            clock.setStateParameters(20.dp, 40.dp)
            clock.setClockTime(0)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            clock.getTransitions(100).let {
                assertTrue(it.isNotEmpty())
                it.forEach { info ->
                    assertTrue(info.startTimeMillis >= 0)
                    assertTrue(info.endTimeMillis >= 0)
                    assertTrue(info.values.isNotEmpty())
                    assertNotNull(info.specType)
                    assertNotNull(info.label)
                }
            }
        }
    }

    @Test
    fun animatedContentClockDuration() {
        val search = AnimationSearch.AnimatedContentSearch { }
        rule.searchForAnimation(search) {
            AnimatedContent(targetState = 1.dp) {}
        }
        val clock = TransitionClock(search.animations.first().parseAnimatedContent()!!)
        rule.runOnIdle {
            assertEquals(0, clock.getMaxDuration())
            assertEquals(0, clock.getMaxDurationPerIteration())
            // Change state
            clock.setStateParameters(20.dp, 40.dp)
        }
        rule.runOnIdle {
            assertTrue(clock.getMaxDuration() >= 100)
            assertTrue(clock.getMaxDurationPerIteration() >= 100)
        }
    }
    //endregion

    fun assertEquals(expected: Int, actual: Int, delta: Int) {
        assertEquals(null, expected.toFloat(), actual.toFloat(), delta.toFloat())
    }

    private fun assertGreaterThanOrEqualTo(min: Int, actual: Int) {
        assertTrue(actual >= min)
    }

    fun assertEquals(expected: Long, actual: Long, delta: Long) {
        assertEquals(null, expected.toFloat(), actual.toFloat(), delta.toFloat())
    }
}