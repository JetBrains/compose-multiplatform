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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SpringSpec
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
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.AnimateXAsStateComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimateXAsStateComposeAnimationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dpAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var dpState: State<Dp>? = null
        rule.searchForAnimation(search) {
            dpState = animateDpAsState(targetValue = 10.dp)
        }
        val composeAnimation = checkDefaultState(search, "DpAnimation", 10.dp)
        rule.runOnUiThread {
            composeAnimation.setState(20.dp)
        }
        rule.waitForIdle()
        assertEquals(20.dp, dpState!!.value)
    }

    @Test
    fun floatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Float>? = null
        rule.searchForAnimation(search) {
            state = animateFloatAsState(targetValue = 10f)
        }
        val composeAnimation = checkDefaultState(search, "FloatAnimation", 10f)
        rule.runOnUiThread {
            composeAnimation.setState(20f)
        }
        rule.waitForIdle()
        assertEquals(20f, state!!.value)
    }

    @Test
    fun intAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Int>? = null
        rule.searchForAnimation(search) {
            state = animateIntAsState(targetValue = 10)
        }
        val composeAnimation = checkDefaultState(search, "IntAnimation", 10)
        rule.runOnUiThread {
            composeAnimation.setState(20)
        }
        rule.waitForIdle()
        assertEquals(20, state!!.value)
    }

    @Test
    fun intSizeAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<IntSize>? = null
        rule.searchForAnimation(search) {
            state = animateIntSizeAsState(targetValue = IntSize(10, 20))
        }
        checkDefaultState(search, "IntSizeAnimation", IntSize(10, 20))
        val composeAnimation = search.animations.first().parse()!!
        rule.runOnUiThread {
            composeAnimation.setState(IntSize(30, 40))
        }
        rule.waitForIdle()
        assertEquals(IntSize(30, 40), state!!.value)
    }

    @Test
    fun intOffsetAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<IntOffset>? = null
        rule.searchForAnimation(search) {
            state = animateIntOffsetAsState(targetValue = IntOffset(10, 20))
        }
        val composeAnimation = checkDefaultState(search, "IntOffsetAnimation", IntOffset(10, 20))
        rule.runOnUiThread {
            composeAnimation.setState(IntOffset(30, 40))
        }
        rule.waitForIdle()
        assertEquals(IntOffset(30, 40), state!!.value)
    }

    @Test
    fun offsetAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Offset>? = null
        rule.searchForAnimation(search) {
            state = animateOffsetAsState(targetValue = Offset(10f, 20f))
        }
        val composeAnimation = checkDefaultState(search, "OffsetAnimation", Offset(10f, 20f))
        rule.runOnUiThread {
            composeAnimation.setState(Offset(30f, 40f))
        }
        rule.waitForIdle()
        assertEquals(Offset(30f, 40f), state!!.value)
    }

    @Test
    fun sizeAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Size>? = null
        rule.searchForAnimation(search) {
            state = animateSizeAsState(targetValue = Size(10f, 20f))
        }
        val composeAnimation = checkDefaultState(search, "SizeAnimation", Size(10f, 20f))
        composeAnimation.setState(Size(30f, 40f))
        rule.waitForIdle()
        assertEquals(Size(30f, 40f), state!!.value)
    }

    @Test
    fun rectAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Rect>? = null
        rule.searchForAnimation(search) {
            state = animateRectAsState(targetValue = Rect(10f, 20f, 30f, 40f))
        }
        val composeAnimation = checkDefaultState(search, "RectAnimation", Rect(10f, 20f, 30f, 40f))
        composeAnimation.setState(Rect(50f, 60f, 70f, 80f))
        rule.waitForIdle()
        assertEquals(Rect(50f, 60f, 70f, 80f), state!!.value)
    }

    @Test
    fun colorAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Color>? = null
        rule.searchForAnimation(search) {
            state = animateColorAsState(targetValue = Color(0.1f, 0.2f, 0.3f, 0.4f))
        }
        val composeAnimation =
            checkDefaultState(search, "ColorAnimation", Color(0.1f, 0.2f, 0.3f, 0.4f))
        composeAnimation.setState(Color(0.3f, 0.4f, 0.5f, 0.6f))
        rule.waitForIdle()
        assertEquals(Color(0.3f, 0.4f, 0.5f, 0.6f), state!!.value)
    }

    @Test
    fun customFloatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch {}
        var state: State<Float>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(10f, Float.VectorConverter)
        }
        val composeAnimation = checkDefaultState(search, "ValueAnimation", 10f)
        composeAnimation.setState(30f)
        rule.waitForIdle()
        assertEquals(30f, state!!.value)
    }

    @Test
    fun nullableFloatAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Float?>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(1f, Utils.nullableFloatConverter)
        }
        val composeAnimation = checkDefaultState(search, "ValueAnimation", 1f)
        composeAnimation.setState(30f)
        rule.waitForIdle()
        assertEquals(30f, state!!.value)
    }

    @Test
    fun nullableFloatAnimationWithNullState() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        rule.searchForAnimation(search) {
            animateValueAsState(null, Utils.nullableFloatConverter)
        }
        assertEquals(1, search.animations.size)
        val composeAnimation = search.animations.first().parse()
        assertNull(composeAnimation)
    }

    @Test
    fun stringAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<String>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState("11.0", Utils.stringConverter)
        }
        val composeAnimation = checkDefaultState(search, "ValueAnimation", "11.0")
        composeAnimation.setState("56.0")
        rule.waitForIdle()
        assertEquals("56.0", state!!.value)
    }

    @Test
    fun enumAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Utils.EnumState>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(Utils.EnumState.One, Utils.enumConverter)
        }
        val composeAnimation = checkDefaultState(search, "ValueAnimation", Utils.EnumState.One, 3)
        composeAnimation.setState(Utils.EnumState.Three)
        rule.waitForIdle()
        assertEquals(Utils.EnumState.Three, state!!.value)
    }

    @Test
    fun nullableEnumAnimation() {
        val search = AnimationSearch.AnimateXAsStateSearch { }
        var state: State<Utils.EnumState?>? = null
        rule.searchForAnimation(search) {
            state = animateValueAsState(Utils.EnumState.One, Utils.nullableEnumConverter)
        }
        val composeAnimation = checkDefaultState(search, "ValueAnimation", Utils.EnumState.One, 3)
        composeAnimation.setState(Utils.EnumState.Three)
        rule.waitForIdle()
        assertEquals(Utils.EnumState.Three, state!!.value)
    }

    private fun checkDefaultState(
        search: AnimationSearch.AnimateXAsStateSearch,
        label: String,
        defaultValue: Any,
        numberOfStates: Int = 1
    ): AnimateXAsStateComposeAnimation<*, *> {
        assertEquals(1, search.animations.size)
        val composeAnimation = search.animations.first().parse()!!
        composeAnimation.animationObject.let {
            assertNotNull(it)
            assertEquals(defaultValue, it.value)
        }
        composeAnimation.animationSpec.let {
            assertNotNull(it)
            assertTrue(it is SpringSpec<*>)
        }
        assertEquals(label, composeAnimation.label)
        assertEquals(numberOfStates, composeAnimation.states.size)
        assertEquals(defaultValue, composeAnimation.states.first())
        assertNotNull(composeAnimation.toolingState)
        return composeAnimation
    }
}