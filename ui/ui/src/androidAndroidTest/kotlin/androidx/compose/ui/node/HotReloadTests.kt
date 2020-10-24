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

@file:Suppress("USELESS_CAST", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.ui.node

import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.clearRoots
import androidx.compose.runtime.emit
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.simulateHotReload
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.accessibilityLabel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.test.filters.MediumTest
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.onNodeWithTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class HotReloadTests {
    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun composeView() {
        lateinit var activity: TestActivity
        rule.activityRule.scenario.onActivity { activity = it }
        var value = "First value"

        @Composable fun text(text: String, id: Int = -1) {
            val context = ContextAmbient.current
            emit<TextView, UiApplier>(
                ctor = { TextView(context) },
                update = {
                    set(id) { this.id = it }
                    set(text) { this.text = it }
                }
            )
        }

        @Composable fun column(children: @Composable () -> Unit) {
            val context = ContextAmbient.current
            emit<LinearLayout, UiApplier>(
                ctor = { LinearLayout(context) },
                update = {},
                children = children
            )
        }

        val composeLatch = CountDownLatch(1)

        rule.runOnUiThread {
            activity.setContent {
                column {
                    text(text = "Hello", id = 101)
                    text(text = "World", id = 102)
                    text(text = value, id = 103)
                }
                onCommit {
                    composeLatch.countDown()
                }
            }
        }

        assertTrue(composeLatch.await(1, TimeUnit.SECONDS))

        assertEquals(activity.findViewById<TextView>(103).text, value)
        value = "Second value"
        assertNotEquals(activity.findViewById<TextView>(103).text, value)

        val hotReloadLatch = CountDownLatch(1)

        rule.runOnUiThread {
            simulateHotReload(activity)
            hotReloadLatch.countDown()
        }

        assertTrue(hotReloadLatch.await(1, TimeUnit.SECONDS))

        assertEquals(activity.findViewById<TextView>(103).text, value)
    }

    @Test
    fun composeLayoutNode() {
        lateinit var activity: TestActivity
        rule.activityRule.scenario.onActivity { activity = it }
        var value = "First value"

        @Composable fun semanticsNode(text: String, id: Int) {
            Box(Modifier.testTag("text$id").semantics { accessibilityLabel = text }) {
            }
        }

        @Composable fun columnNode(children: @Composable () -> Unit) {
            children()
        }

        val composeLatch = CountDownLatch(1)

        // Set the content of the view
        rule.runOnUiThread {
            activity.setContent {
                columnNode {
                    semanticsNode(text = value, id = 103)
                }
                onCommit {
                    composeLatch.countDown()
                }
            }
        }

        assertTrue(composeLatch.await(1, TimeUnit.SECONDS))

        fun target() = rule.onNodeWithTag("text103")

        // Assert that the composition has the correct value
        target().assertLabelEquals(value)

        value = "Second value"

        val hotReloadLatch = CountDownLatch(1)

        // Simulate hot-reload
        rule.runOnUiThread {
            simulateHotReload(activity)
            hotReloadLatch.countDown()
        }

        assertTrue(hotReloadLatch.await(1, TimeUnit.SECONDS))

        // Detect that the node changed
        target().assertLabelEquals(value)
    }
}
