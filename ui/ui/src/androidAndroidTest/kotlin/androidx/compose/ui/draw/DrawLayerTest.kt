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

package androidx.compose.ui.draw

import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Padding
import androidx.compose.ui.PaddingModifier
import androidx.compose.ui.TransformOrigin
import androidx.compose.ui.drawLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.globalBounds
import androidx.compose.ui.layout.globalPosition
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class DrawLayerTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    private lateinit var activity: TestActivity
    private lateinit var positionLatch: CountDownLatch
    private lateinit var layoutCoordinates: LayoutCoordinates

    private val positioner = Modifier.onGloballyPositioned {
        layoutCoordinates = it
        positionLatch.countDown()
    }

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        positionLatch = CountDownLatch(1)
    }

    @Test
    fun testLayerBoundsPosition() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30, PaddingModifier(10).drawLayer().then(positioner)) {
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            assertEquals(Offset(10f, 10f), layoutCoordinates.positionInRoot)
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(10f, 10f, 40f, 40f), bounds)
            val global = layoutCoordinates.globalBounds
            val position = layoutCoordinates.globalPosition
            assertEquals(position.x, global.left)
            assertEquals(position.y, global.top)
            assertEquals(30f, global.width)
            assertEquals(30f, global.height)
        }
    }

    @Test
    fun testScale() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(
                        10,
                        Modifier.drawLayer(scaleX = 2f, scaleY = 3f).then(positioner)
                    ) {
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(5f, 0f, 25f, 30f), bounds)
            assertEquals(Offset(5f, 0f), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testRotation() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(
                        10,
                        Modifier.drawLayer(scaleY = 3f, rotationZ = 90f).then(positioner)
                    ) {
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(0f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testRotationPivot() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(
                        10,
                        Modifier.drawLayer(
                            rotationZ = 90f,
                            transformOrigin = TransformOrigin(1.0f, 1.0f)
                        ).then(positioner)
                    )
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(20f, 10f, 30f, 20f), bounds)
            assertEquals(Offset(30f, 10f), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testTranslationXY() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(
                        10,
                        Modifier.drawLayer(
                            translationX = 5.0f,
                            translationY = 8.0f
                        ).then(positioner)
                    )
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(15f, 18f, 25f, 28f), bounds)
            assertEquals(Offset(15f, 18f), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testClip() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(10, Modifier.drawLayer(clip = true)) {
                        FixedSize(
                            10,
                            Modifier.drawLayer(scaleX = 2f).then(positioner)
                        ) {
                        }
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(Rect(10f, 10f, 20f, 20f), bounds)
            // Positions aren't clipped
            assertEquals(Offset(5f, 10f), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testTotalClip() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10) {
                    FixedSize(10, Modifier.drawLayer(clip = true)) {
                        FixedSize(
                            10,
                            PaddingModifier(20).then(positioner)
                        ) {
                        }
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            // should be completely clipped out
            assertEquals(0f, bounds.width)
            assertEquals(0f, bounds.height)
        }
    }
}