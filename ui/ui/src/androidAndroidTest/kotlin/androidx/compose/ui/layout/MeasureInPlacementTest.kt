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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class MeasureInPlacementTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setup() {
        rule.activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    /**
     * Make sure that measurement in the layout modifier's placement block doesn't crash.
     */
    @Test
    fun measureInModifierPlacement() {
        var childSize = IntSize.Zero
        rule.setContent {
            val measureInPlaceModifier = Modifier.layout { measurable, constraints ->
                layout(100, 100) {
                    val p = measurable.measure(constraints)
                    childSize = IntSize(p.width, p.height)
                    p.place(0, 0)
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .then(measureInPlaceModifier)
            ) {
                Box(Modifier.size(10.dp))
            }
        }

        rule.waitForIdle()
        assertThat(childSize.width).isGreaterThan(0)
        assertThat(childSize.height).isGreaterThan(0)
    }

    /**
     * Make sure that measurement in the layout's placement block doesn't crash.
     */
    @Test
    fun measureInLayoutPlacement() {
        var childSize = IntSize.Zero
        rule.setContent {
            Layout(modifier = Modifier.fillMaxSize(), content = @Composable {
                Box(Modifier.size(10.dp))
            }) { measurables, constraints ->
                layout(100, 100) {
                    val p = measurables[0].measure(constraints)
                    childSize = IntSize(p.width, p.height)
                    p.place(0, 0)
                }
            }
        }

        rule.waitForIdle()
        assertThat(childSize.width).isGreaterThan(0)
        assertThat(childSize.height).isGreaterThan(0)
    }

    /**
     * Make sure that measurement in the layout modifier's placement block doesn't crash when
     * LookaheadLayout is used.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun measureInModifierPlacementWithLookaheadLayout() {
        var childSize = IntSize.Zero
        rule.setContent {
            LookaheadLayout(content = @Composable {
                val measureInPlaceModifier = Modifier.layout { measurable, constraints ->
                    layout(100, 100) {
                        val p = measurable.measure(constraints)
                        childSize = IntSize(p.width, p.height)
                        p.place(0, 0)
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(measureInPlaceModifier)
                ) {
                    Box(Modifier.size(10.dp))
                }
            }, measurePolicy = { measurables, constraints ->
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0, 0)
                }
            })
        }

        rule.waitForIdle()
        assertThat(childSize.width).isGreaterThan(0)
        assertThat(childSize.height).isGreaterThan(0)
    }

    /**
     * Make sure that measurement in the layout's placement block doesn't crash when
     * LookaheadLayout is used.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun measureInLayoutPlacementWithLookaheadLayout() {
        var childSize = IntSize.Zero
        rule.setContent {
            LookaheadLayout(content = @Composable {
                Layout(modifier = Modifier.fillMaxSize(), content = @Composable {
                    Box(Modifier.size(10.dp))
                }) { measurables, constraints ->
                    layout(100, 100) {
                        val p = measurables[0].measure(constraints)
                        childSize = IntSize(p.width, p.height)
                        p.place(0, 0)
                    }
                }
            }, measurePolicy = { measurables, constraints ->
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0, 0)
                }
            })
        }

        rule.waitForIdle()
        assertThat(childSize.width).isGreaterThan(0)
        assertThat(childSize.height).isGreaterThan(0)
    }

    @Test
    fun remeasureRequestForANodeWhichIsNotYetPlacedButMeasuredAlready() {
        var needToMeasureTopBar by mutableStateOf(false)
        var topBoxSize by mutableStateOf(0.dp)
        val stateBasedSize = Modifier.layout { measurable, _ ->
            val sizePx = topBoxSize.roundToPx()
            val placeable = measurable.measure(Constraints.fixed(sizePx, sizePx))
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
        rule.setContent {
            Layout(
                content = {
                    Box(stateBasedSize.testTag("top"))
                    Box(Modifier.size(10.dp).testTag("bottom"))
                }
            ) { measurables, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val topBarHeight = if (needToMeasureTopBar) {
                        val placeable = measurables[0].measure(Constraints())
                        if (Snapshot.withoutReadObservation { topBoxSize } == 0.dp) {
                            topBoxSize = 10.dp
                            // it will synchronously request one more remeasure for measurables[0]
                            // while it is still not placed. such requests were ignored previously
                            // meaning that given remeasure will never happen.
                            Snapshot.sendApplyNotifications()
                        }
                        placeable.place(0, 0)
                        placeable.height
                    } else {
                        0
                    }
                    measurables[1].measure(Constraints()).place(0, topBarHeight)
                }
            }
        }

        rule.runOnIdle {
            needToMeasureTopBar = true
        }

        rule.onNodeWithTag("bottom")
            .assertIsDisplayed()
            .assertPositionInRootIsEqualTo(0.dp, 10.dp)
        rule.onNodeWithTag("top")
            .assertIsDisplayed()
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
    }
}
