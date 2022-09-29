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

package androidx.compose.material.swipeable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableV2State
import androidx.compose.material.rememberSwipeableV2State
import androidx.compose.material.swipeAnchors
import androidx.compose.material.swipeable.TestState.A
import androidx.compose.material.swipeable.TestState.B
import androidx.compose.material.swipeable.TestState.C
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalMaterialApi::class)
class SwipeableV2AnchorTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun swipeable_swipeAnchors_updatedInSameFrame() {
        rule.mainClock.autoAdvance = false

        var compositionCounter = 0
        val state = SwipeableV2State(initialState = A)

        rule.setContent {
            compositionCounter++
            Box(
                Modifier
                    .height(200.dp)
                    .swipeAnchors(
                        state,
                        possibleStates = setOf(A, B, C)
                    ) { state, layoutSize ->
                        when (state) {
                            A -> 0f
                            B -> layoutSize.height.toFloat() / 2
                            C -> layoutSize.height.toFloat()
                        }
                    }
            )
        }
        // Verify composed initially but didn't recompose
        assertThat(compositionCounter).isEqualTo(1)
        // Verify that the anchors are present after one composition
        assertThat(state.anchors).containsKey(A)
        assertThat(state.anchors).containsKey(B)
        assertThat(state.anchors).containsKey(C)
    }

    @Test
    fun swipeable_swipeAnchors_calculatedCorrectlyFromLayoutSize() {
        val state = SwipeableV2State(initialState = A)

        fun anchorA() = 0f
        fun anchorB(layoutHeight: Float) = layoutHeight / 2
        fun anchorC(layoutHeight: Float) = layoutHeight

        val swipeableSize = 200.dp

        rule.setContent {
            Box(
                Modifier
                    .requiredHeight(swipeableSize)
                    .swipeAnchors(
                        state,
                        possibleStates = setOf(A, B, C)
                    ) { state, layoutSize ->
                        when (state) {
                            A -> 0f
                            B -> anchorB(layoutSize.height.toFloat())
                            C -> anchorC(layoutSize.height.toFloat())
                        }
                    }
            )
        }

        val expectedHeight = with(rule.density) { swipeableSize.toPx() }
        assertThat(state.anchors[A]).isEqualTo(anchorA())
        assertThat(state.anchors[B]).isEqualTo(anchorB(expectedHeight))
        assertThat(state.anchors[C]).isEqualTo(anchorC(expectedHeight))
    }

    @Test
    fun swipeable_swipeAnchors_updateOnSizeChange() {
        lateinit var state: SwipeableV2State<TestState>

        val firstAnchors = mapOf(A to 0f, B to 100f, C to 200f)
        val secondAnchors = mapOf(B to 300f, C to 600f)
        var anchors = firstAnchors
        var size by mutableStateOf(100)

        rule.setContent {
            state = rememberSwipeableV2State(A)
            Box(
                Modifier
                    .size(size.dp) // Trigger remeasure when size changes
                    .swipeAnchors(
                        state,
                        possibleStates = setOf(A, B, C),
                        calculateAnchor = { state, _ -> anchors[state] }
                    )
            )
        }

        assertThat(state.anchors).isEqualTo(firstAnchors)

        anchors = secondAnchors
        size = 200
        rule.waitForIdle()

        assertThat(state.anchors).isEqualTo(secondAnchors)
    }
}