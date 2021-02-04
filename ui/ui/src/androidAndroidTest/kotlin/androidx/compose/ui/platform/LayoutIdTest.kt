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

package androidx.compose.ui.platform

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class LayoutIdTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testTags() {
        val latch = CountDownLatch(1)
        rule.runOnUiThreadIR {
            activity.setContent {
                Layout(
                    {
                        AtLeastSize(0, Modifier.layoutId("first"), content = {})
                        Box(Modifier.layoutId("second")) {
                            AtLeastSize(
                                0,
                                content = {}
                            )
                        }
                        Box(Modifier.layoutId("third")) {
                            AtLeastSize(0, content = {})
                        }
                    }
                ) { measurables, _ ->
                    assertEquals(3, measurables.size)
                    assertEquals("first", measurables[0].layoutId)
                    assertEquals("second", measurables[1].layoutId)
                    assertEquals("third", measurables[2].layoutId)
                    latch.countDown()
                    layout(0, 0) {}
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testInspectableValue() {
        val modifier = Modifier.layoutId("box") as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("layoutId")
        assertThat(modifier.valueOverride).isEqualTo("box")
        assertThat(modifier.inspectableElements.asIterable()).isEmpty()
    }
}