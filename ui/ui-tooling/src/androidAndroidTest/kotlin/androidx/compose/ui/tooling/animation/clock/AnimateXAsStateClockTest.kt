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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.Utils
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.tooling.animation.states.ComposeAnimationState
import androidx.compose.ui.tooling.animation.states.TargetState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimateXAsStateClockTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dpAnimationClock() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Dp>? = null
        rule.searchForAnimation(search) {
            state = animateDpAsState(
                targetValue = 10.dp, animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "DpAnimation",
            initialValue = 10.dp,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(1.dp, 2.dp) }
        checkUpdatedState(clock, label = "DpAnimation",
            newInitialValue = 1.dp, newTargetValue = 2.dp,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(3f, 4.0) }
        checkUpdatedState(clock, label = "DpAnimation",
            newInitialValue = 3.dp, newTargetValue = 4.dp,
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(listOf(30f), listOf(40f))
        }
        checkUpdatedState(clock, label = "DpAnimation",
            newInitialValue = 30.dp, newTargetValue = 40.dp,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(111.dp, "")
            clock.setStateParameters(111.dp, null)
            clock.setStateParameters(listOf(111.dp), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(111.dp), emptyList<Dp>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "DpAnimation",
            newInitialValue = 30.dp, newTargetValue = 40.dp,
            composeState = { state!!.value })
    }

    @Test
    fun floatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Float>? = null
        rule.searchForAnimation(search) {
            state = animateFloatAsState(
                targetValue = 10f, animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "FloatAnimation",
            initialValue = 10f,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(1f, 2f) }
        checkUpdatedState(clock, label = "FloatAnimation",
            newInitialValue = 1f, newTargetValue = 2f,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3f), listOf(4f)) }
        checkUpdatedState(clock, label = "FloatAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(111f, 111)
            clock.setStateParameters(111f, null)
            clock.setStateParameters(listOf(111f), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(111f), emptyList<Dp>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "FloatAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value })
    }

    @Test
    fun intSizeAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<IntSize>? = null
        rule.searchForAnimation(search) {
            state = animateIntSizeAsState(
                targetValue = IntSize(10, 20),
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "IntSizeAnimation",
            initialValue = IntSize(10, 20),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(IntSize(3, 4), IntSize(4, 5))
        }
        checkUpdatedState(clock, label = "IntSizeAnimation",
            newInitialValue = IntSize(3, 4), newTargetValue = IntSize(4, 5),
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(5, 6), listOf(7, 8)) }
        checkUpdatedState(clock, label = "IntSizeAnimation",
            newInitialValue = IntSize(5, 6), newTargetValue = IntSize(7, 8),
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(IntSize(111, 111), 111)
            clock.setStateParameters(IntSize(111, 111), null)
            clock.setStateParameters(10, 10)
            clock.setStateParameters(listOf(IntSize(111, 111)), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(IntSize(111, 11)), emptyList<IntOffset>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "IntSizeAnimation",
            newInitialValue = IntSize(5, 6), newTargetValue = IntSize(7, 8),
            composeState = { state!!.value })
    }

    @Test
    fun intAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Int>? = null
        rule.searchForAnimation(search) {
            state = animateIntAsState(
                targetValue = 10, animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "IntAnimation",
            initialValue = 10,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(1, 2) }
        checkUpdatedState(clock, label = "IntAnimation",
            newInitialValue = 1, newTargetValue = 2,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3), listOf(4)) }
        checkUpdatedState(clock, label = "IntAnimation",
            newInitialValue = 3, newTargetValue = 4,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(111, 111f)
            clock.setStateParameters(111, null)
            clock.setStateParameters(111f, 111f)
            clock.setStateParameters(listOf(111), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(111), emptyList<Int>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "IntAnimation",
            newInitialValue = 3, newTargetValue = 4,
            composeState = { state!!.value })
    }

    @Test
    fun intOffsetAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<IntOffset>? = null
        rule.searchForAnimation(search) {
            state = animateIntOffsetAsState(
                targetValue = IntOffset(10, 20),
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "IntOffsetAnimation",
            initialValue = IntOffset(10, 20),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(IntOffset(1, 2), IntOffset(3, 4))
        }
        checkUpdatedState(clock, label = "IntOffsetAnimation",
            newInitialValue = IntOffset(1, 2), newTargetValue = IntOffset(3, 4),
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3, 4), listOf(4, 5)) }
        checkUpdatedState(clock, label = "IntOffsetAnimation",
            newInitialValue = IntOffset(3, 4), newTargetValue = IntOffset(4, 5),
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(IntOffset(111, 111), 111)
            clock.setStateParameters(IntOffset(111, 111), null)
            clock.setStateParameters(10, 10)
            clock.setStateParameters(listOf(IntOffset(111, 111)), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(IntOffset(111, 11)), emptyList<IntOffset>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "IntOffsetAnimation",
            newInitialValue = IntOffset(3, 4), newTargetValue = IntOffset(4, 5),
            composeState = { state!!.value })
    }

    @Test
    fun offsetAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Offset>? = null
        rule.searchForAnimation(search) {
            state = animateOffsetAsState(
                targetValue = Offset(10f, 20f),
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "OffsetAnimation",
            initialValue = Offset(10f, 20f),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(Offset(1f, 2f), Offset(3f, 4f))
        }
        checkUpdatedState(clock, label = "OffsetAnimation",
            newInitialValue = Offset(1f, 2f), newTargetValue = Offset(3f, 4f),
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3f, 4f), listOf(4f, 5f)) }
        checkUpdatedState(clock, label = "OffsetAnimation",
            newInitialValue = Offset(3f, 4f), newTargetValue = Offset(4f, 5f),
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(Offset(111f, 111f), 111)
            clock.setStateParameters(Offset(111f, 111f), null)
            clock.setStateParameters(10, 10)
            clock.setStateParameters(listOf(Offset(111f, 111f)), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(Offset(111f, 111f)), emptyList<Offset>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "OffsetAnimation",
            newInitialValue = Offset(3f, 4f), newTargetValue = Offset(4f, 5f),
            composeState = { state!!.value })
    }

    @Test
    fun sizeAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Size>? = null
        rule.searchForAnimation(search) {
            state = animateSizeAsState(
                targetValue = Size(10f, 20f),
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "SizeAnimation",
            initialValue = Size(10f, 20f),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(Size(1f, 2f), Size(3f, 4f))
        }
        checkUpdatedState(clock, label = "SizeAnimation",
            newInitialValue = Size(1f, 2f), newTargetValue = Size(3f, 4f),
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3f, 4f), listOf(4f, 5f)) }
        checkUpdatedState(clock, label = "SizeAnimation",
            newInitialValue = Size(3f, 4f), newTargetValue = Size(4f, 5f),
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(Size(111f, 111f), 111)
            clock.setStateParameters(Size(111f, 111f), null)
            clock.setStateParameters(10, 10)
            clock.setStateParameters(listOf(Size(111f, 111f)), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(Size(111f, 111f)), emptyList<Size>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "SizeAnimation",
            newInitialValue = Size(3f, 4f), newTargetValue = Size(4f, 5f),
            composeState = { state!!.value })
    }

    @Test
    fun rectAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Rect>? = null
        rule.searchForAnimation(search) {
            state = animateRectAsState(
                targetValue = Rect(10f, 20f, 30f, 40f),
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "RectAnimation",
            initialValue = Rect(10f, 20f, 30f, 40f),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(
                Rect(1f, 2f, 30f, 40f),
                Rect(3f, 4f, 30f, 40f)
            )
        }
        checkUpdatedState(clock, label = "RectAnimation",
            newInitialValue = Rect(1f, 2f, 30f, 40f),
            newTargetValue = Rect(3f, 4f, 30f, 40f),
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(
                listOf(3f, 4f, 30f, 40f),
                listOf(4f, 5f, 30f, 40f)
            )
        }
        checkUpdatedState(clock, label = "RectAnimation",
            newInitialValue = Rect(3f, 4f, 30f, 40f),
            newTargetValue = Rect(4f, 5f, 30f, 40f),
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(Rect(42f, 42f, 42f, 42f), 42f)
            clock.setStateParameters(Rect(42f, 42f, 42f, 42f), null)
            clock.setStateParameters(42f, 42f)
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(Rect(42f, 42f, 42f, 42f)), listOf(42f))
            clock.setStateParameters(listOf(Rect(42f, 42f, 42f, 42f)), emptyList<Rect>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "RectAnimation",
            newInitialValue = Rect(3f, 4f, 30f, 40f),
            newTargetValue = Rect(4f, 5f, 30f, 40f),
            composeState = { state!!.value })
    }

    @Test
    fun colorAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Color>? = null
        rule.searchForAnimation(search) {
            state = animateColorAsState(
                targetValue = Color.Black,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ColorAnimation",
            initialValue = Color.Black,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(Color.Gray, Color.Red) }
        checkUpdatedState(clock, label = "ColorAnimation",
            newInitialValue = Color.Gray, newTargetValue = Color.Red,
            composeState = { state!!.value })
        rule.runOnUiThread {
            clock.setStateParameters(
                listOf(Color.Blue.red, Color.Blue.green, Color.Blue.blue, Color.Blue.alpha),
                listOf(Color.Yellow.red, Color.Yellow.green, Color.Yellow.blue, Color.Yellow.alpha)
            )
        }
        checkUpdatedState(clock, label = "ColorAnimation",
            newInitialValue = Color.Blue, newTargetValue = Color.Yellow,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(Color.Red, 1)
            clock.setStateParameters(Color.Red, null)
            clock.setStateParameters(10, 10)
            clock.setStateParameters(listOf(Color.Red), listOf(0.1f, 0.2f, 0.3f))
            clock.setStateParameters(listOf(Color.Red), emptyList<Color>())
            clock.setStateParameters(listOf(null), listOf(null))
            // Invalid arguments for color.
            clock.setStateParameters(listOf(10f, 10f, 10f, 10f), listOf(10f, 10f, 10f, 10f))
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "ColorAnimation",
            newInitialValue = Color.Blue, newTargetValue = Color.Yellow,
            composeState = { state!!.value })
    }

    @Test
    fun customFloatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Float>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = 10f,
                Float.VectorConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = 10f,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(1f, 2f) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 1f, newTargetValue = 2f,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(3f), listOf(4f)) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(30f, 40)
            clock.setStateParameters(30f, null)
            clock.setStateParameters(30L, 40L)
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(30f), listOf(40L))
            clock.setStateParameters(emptyList<Float>(), emptyList<Float>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value })
    }

    @Test
    fun nullableFloatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Float?>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = 10f,
                Utils.nullableFloatConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = 10f,
            composeState = { state!!.value!! })
        rule.runOnUiThread { clock.setStateParameters(1f, 2f) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 1f, newTargetValue = 2f,
            composeState = { state!!.value!! })
        rule.runOnUiThread { clock.setStateParameters(listOf(3f), listOf(4f)) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value!! })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(30f, 40)
            clock.setStateParameters(30f, null)
            clock.setStateParameters(30L, 40L)
            clock.setStateParameters(listOf(30f), listOf(40L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(emptyList<Float>(), emptyList<Float>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = 3f, newTargetValue = 4f,
            composeState = { state!!.value!! })
    }

    @Test
    fun stringAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<String>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = "10.0",
                Utils.stringConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = "10.0",
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters("20.0", "30.0") }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = "20.0", newTargetValue = "30.0",
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf("40.0"), listOf("50.0")) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = "40.0", newTargetValue = "50.0",
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(30f, 40)
            clock.setStateParameters(30f, null)
            clock.setStateParameters(30f, 40f)
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(30f), listOf(40L))
            clock.setStateParameters(emptyList<String>(), emptyList<Int>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = "40.0", newTargetValue = "50.0",
            composeState = { state!!.value })
    }

    @Test
    fun enumAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Utils.EnumState>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = Utils.EnumState.One,
                Utils.enumConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = Utils.EnumState.One,
            composeState = { state!!.value })
    }

    @Test
    fun booleanAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Boolean>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = false,
                Utils.booleanConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = false,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(false, true) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = false, newTargetValue = true,
            composeState = { state!!.value })
        rule.runOnUiThread { clock.setStateParameters(listOf(true), listOf(false)) }
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = true, newTargetValue = false,
            composeState = { state!!.value })
        // Invalid parameters are ignored.
        rule.runOnUiThread {
            clock.setStateParameters(true, 111f)
            clock.setStateParameters(true, null)
            clock.setStateParameters(42f, 42f)
            clock.setStateParameters(listOf(true), listOf(111L))
            clock.setStateParameters(listOf(null), listOf(null))
            clock.setStateParameters(listOf(true), emptyList<Boolean>())
        }
        // State hasn't changed.
        checkUpdatedState(clock, label = "ValueAnimation",
            newInitialValue = true, newTargetValue = false,
            composeState = { state!!.value })
    }

    @Test
    fun nullableEnumAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Utils.EnumState?>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(
                targetValue = Utils.EnumState.One,
                Utils.nullableEnumConverter,
                animationSpec = TweenSpec(durationMillis = 100)
            )
        }
        val clock = AnimateXAsStateClock(search.animations.first().parse()!!)
        checkInitialState(clock, label = "ValueAnimation",
            initialValue = Utils.EnumState.One,
            composeState = { state!!.value!! })
    }

    private fun <T : ComposeAnimation, TState : ComposeAnimationState, V : Any>
        checkInitialState(
        clock: ComposeAnimationClock<T, TState>,
        label: String,
        initialValue: V,
        composeState: () -> V
    ) {
        // Check default state.
        assertEquals(initialValue, composeState())
        assertEquals(TargetState(initialValue, initialValue), clock.state)
        assertEquals(100L, clock.getMaxDuration())
        assertEquals(100L, clock.getMaxDurationPerIteration())
        assertEquals(
            listOf(ComposeAnimatedProperty(label, initialValue)),
            clock.getAnimatedProperties()
        )
        val transitions = clock.getTransitions(100)
        assertEquals(1, transitions.size)
        transitions.first().let {
            assertEquals(label, it.label)
            assertEquals(0L, it.startTimeMillis)
            assertEquals(100L, it.endTimeMillis)
            assertEquals("androidx.compose.animation.core.TweenSpec", it.specType)
            assertEquals(mapOf(0L to initialValue, 100L to initialValue), it.values)
        }
    }

    private fun <T : ComposeAnimation, TState : ComposeAnimationState, V : Any>
        checkUpdatedState(
        clock: ComposeAnimationClock<T, TState>,
        label: String,
        newInitialValue: V,
        newTargetValue: V,
        composeState: () -> V
    ) {
        rule.waitForIdle()
        // Check new state.
        rule.runOnUiThread {
            clock.setClockTime(0)
            assertEquals(newInitialValue, composeState())
            assertEquals(TargetState(newInitialValue, newTargetValue), clock.state)
            assertEquals(100L, clock.getMaxDuration())
            assertEquals(100L, clock.getMaxDurationPerIteration())
            assertEquals(
                listOf(ComposeAnimatedProperty(label, newInitialValue)),
                clock.getAnimatedProperties()
            )
            val newTransitions = clock.getTransitions(100)
            assertEquals(1, newTransitions.size)
            newTransitions.first().let {
                assertEquals(label, it.label)
                assertEquals(0L, it.startTimeMillis)
                assertEquals(100L, it.endTimeMillis)
                assertEquals("androidx.compose.animation.core.TweenSpec", it.specType)
                assertEquals(mapOf(0L to newInitialValue, 100L to newTargetValue), it.values)
            }
        }
        // Jump to the end of the animation.
        rule.runOnUiThread {
            clock.setClockTime(millisToNanos(300L))
        }
        // Check state at the end of the animation.
        rule.waitForIdle()
        rule.runOnUiThread {
            assertEquals(newTargetValue, composeState())
            val properties = clock.getAnimatedProperties()
            assertEquals(listOf(ComposeAnimatedProperty(label, newTargetValue)), properties)
        }
    }
}