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

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.simulateHotReload
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class HotReloadTests {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun composeLayoutNode() {
        lateinit var activity: TestActivity
        rule.activityRule.scenario.onActivity { activity = it }
        var value = "First value"

        @Composable fun semanticsNode(text: String, id: Int) {
            Box(Modifier.testTag("text$id").semantics { contentDescription = text }) {
            }
        }

        @Composable fun columnNode(content: @Composable () -> Unit) {
            content()
        }

        val composeLatch = CountDownLatch(1)

        // Set the content of the view
        rule.runOnUiThread {
            activity.setContent {
                columnNode {
                    semanticsNode(text = value, id = 103)
                }
                SideEffect {
                    composeLatch.countDown()
                }
            }
        }

        assertTrue(composeLatch.await(1, TimeUnit.SECONDS))

        fun target() = rule.onNodeWithTag("text103")

        // Assert that the composition has the correct value
        target().assertContentDescriptionEquals(value)

        value = "Second value"

        val hotReloadLatch = CountDownLatch(1)

        // Simulate hot-reload
        rule.runOnUiThread {
            simulateHotReload(activity)
            hotReloadLatch.countDown()
        }

        assertTrue(hotReloadLatch.await(1, TimeUnit.SECONDS))

        // Detect that the node changed
        target().assertContentDescriptionEquals(value)
    }
}
