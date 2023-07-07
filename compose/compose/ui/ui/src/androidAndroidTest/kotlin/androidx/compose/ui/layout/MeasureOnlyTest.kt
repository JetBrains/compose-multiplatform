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

import android.view.View
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.RootMeasurePolicy.measure
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MeasureOnlyTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * onMeasure() shouldn't call placement or onPlace() or onGloballyPositioned()
     */
    @Test
    fun onMeasureDoesNotPlace() {
        var onPlacedCalled: Boolean
        var onGloballyPositionedCalled: Boolean
        var placementCalled: Boolean
        lateinit var view: View

        rule.setContent {
            view = LocalView.current
            Layout(modifier = Modifier
                .fillMaxSize()
                .background(Color.Blue)
                .onPlaced { onPlacedCalled = true }
                .onGloballyPositioned { onGloballyPositionedCalled = true }
            ) { _, constraints ->
                val width = constraints.constrainWidth(10000)
                val height = constraints.constrainHeight(10000)
                layout(width, height) {
                    placementCalled = true
                }
            }
        }

        rule.runOnIdle {
            onPlacedCalled = false
            onGloballyPositionedCalled = false
            placementCalled = false
            val widthSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            assertThat(onPlacedCalled).isFalse()
            assertThat(onGloballyPositionedCalled).isFalse()
            assertThat(placementCalled).isFalse()
        }
    }

    @Test
    fun childrenRequiredForMeasurementRemeasured() {
        lateinit var view: View
        var childMeasured: Boolean

        rule.setContent {
            view = LocalView.current
            val child = @Composable {
                Layout { _, _ ->
                    childMeasured = true
                    layout(10, 10) { }
                }
            }
            Layout(
                content = child,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
            ) { measurables, constraints ->
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            childMeasured = false
            val widthSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            assertThat(childMeasured).isTrue()
        }
    }

    @Test
    fun childrenNotRequiredForMeasurementNotRemeasured() {
        lateinit var view: View
        var childMeasured: Boolean

        rule.setContent {
            view = LocalView.current
            val child = @Composable {
                Layout { _, _ ->
                    childMeasured = true
                    layout(10, 10) { }
                }
            }
            Layout(
                content = child,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
            ) { measurables, constraints ->
                layout(100, 100) {
                    val p = measurables[0].measure(constraints)
                    p.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            childMeasured = false
            val widthSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY)
            view.measure(widthSpec, heightSpec)
            assertThat(childMeasured).isFalse()
        }
    }

    @Test
    fun invalidatedChildRequiredForMeasurementRemeasured() {
        lateinit var view: View
        var childMeasured: Boolean
        var childSize by mutableStateOf(IntSize(10, 10))

        rule.setContent {
            view = LocalView.current
            val child = @Composable {
                Layout { _, _ ->
                    childMeasured = true
                    layout(childSize.width, childSize.height) { }
                }
            }
            Layout(
                content = child,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
            ) { measurables, constraints ->
                val p = measurables[0].measure(constraints)
                layout(p.width, p.height) {
                    p.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            val widthSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST)
            val heightSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.width, view.height)
        }
        rule.runOnIdle {
            // Must change the MeasureSpec or measure() won't call onMeasure()
            val widthSpec = MeasureSpec.makeMeasureSpec(101, MeasureSpec.AT_MOST)
            val heightSpec = MeasureSpec.makeMeasureSpec(101, MeasureSpec.AT_MOST)
            childMeasured = false
            childSize = IntSize(12, 12)
            Snapshot.sendApplyNotifications()
            view.measure(widthSpec, heightSpec)
            assertThat(childMeasured).isTrue()
        }
    }

    @Test
    fun invalidatedChildNotRequiredForMeasurementNotRemeasured() {
        lateinit var view: View
        var childMeasured: Boolean
        var childSize by mutableStateOf(IntSize(10, 10))

        rule.setContent {
            view = LocalView.current
            val child = @Composable {
                Layout { _, _ ->
                    childMeasured = true
                    layout(childSize.width, childSize.height) { }
                }
            }
            Layout(
                content = child,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
            ) { measurables, constraints ->
                layout(10, 10) {
                    val p = measurables[0].measure(constraints)
                    p.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            val widthSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST)
            val heightSpec = MeasureSpec.makeMeasureSpec(100, MeasureSpec.AT_MOST)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.width, view.height)
        }
        rule.runOnIdle {
            // Must change the MeasureSpec or measure() won't call onMeasure()
            val widthSpec = MeasureSpec.makeMeasureSpec(101, MeasureSpec.AT_MOST)
            val heightSpec = MeasureSpec.makeMeasureSpec(101, MeasureSpec.AT_MOST)
            childMeasured = false
            childSize = IntSize(12, 12)
            Snapshot.sendApplyNotifications()
            view.measure(widthSpec, heightSpec)
            assertThat(childMeasured).isFalse()
        }
    }

    /**
     * When a descendant affects the root size, the root should resize when the
     * descendant changes size.
     */
    @Test
    fun remeasureRoot() {
        lateinit var view: View
        var showContent by mutableStateOf(false)
        rule.setContent {
            view = LocalView.current
            AndroidView(factory = { context ->
                ComposeView(context).apply {
                    setContent {
                        Box {
                            Layout { _, _ ->
                                val size = if (showContent) 10 else 0
                                layout(size, size) {}
                            }
                        }
                    }
                }
            })
        }
        rule.runOnIdle {
            assertThat(view.height).isEqualTo(0)
            showContent = true
        }
        rule.runOnIdle {
            assertThat(view.height).isEqualTo(10)
        }
    }
}