/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.Ref
import androidx.compose.ui.test.TestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class ParentDataModifierTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    // Test that parent data defaults to null
    @Test
    fun parentDataDefaultsToNull() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                Layout(
                    content = {
                        SimpleDrawChild(drawLatch = drawLatch)
                    },
                    measurePolicy = { measurables, constraints ->
                        assertEquals(1, measurables.size)
                        parentData.value = measurables[0].parentData

                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
    }

    // Test that parent data doesn't flow to grandchild measurables. They must be
    // reset on every Layout level
    @Test
    fun parentDataIsReset() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                Layout(
                    modifier = Modifier.layoutId("Hello"),
                    content = {
                        SimpleDrawChild(drawLatch = drawLatch)
                    },
                    measurePolicy = { measurables, constraints ->
                        assertEquals(1, measurables.size)
                        parentData.value = measurables[0].parentData

                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
    }

    @Test
    fun multiChildLayoutTest_doesNotOverrideChildrenParentData() {
        runOnUiThread {
            activity.setContent {
                val header = @Composable {
                    Layout(
                        modifier = Modifier.layoutId(0),
                        content = {}
                    ) { _, _ -> layout(0, 0) {} }
                }
                val footer = @Composable {
                    Layout(
                        modifier = Modifier.layoutId(1),
                        content = {}
                    ) { _, _ -> layout(0, 0) {} }
                }

                Layout({ header(); footer() }) { measurables, _ ->
                    assertEquals(0, ((measurables[0]).parentData as? LayoutIdParentData)?.layoutId)
                    assertEquals(1, ((measurables[1]).parentData as? LayoutIdParentData)?.layoutId)
                    layout(0, 0) { }
                }
            }
        }
    }

    @Test
    fun parentDataOnPlaceable() {
        runOnUiThread {
            activity.setContent {
                Layout({
                    Layout(
                        modifier = Modifier.layoutId("data"),
                        content = {}
                    ) { _, _ -> layout(0, 0) {} }
                }) { measurables, constraints ->
                    val placeable = measurables[0].measure(constraints)
                    assertEquals("data", (placeable.parentData as? LayoutIdParentData)?.layoutId)
                    layout(0, 0) { }
                }
            }
        }
    }

    // We only need this because IR compiler doesn't like converting lambdas to Runnables
    private fun runOnUiThread(block: () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                block()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}

@Composable
fun SimpleDrawChild(drawLatch: CountDownLatch) {
    AtLeastSize(
        size = 10,
        modifier = Modifier.drawBehind {
            drawRect(Color(0xFF008000))
            drawLatch.countDown()
        }
    ) {}
}
