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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class DrawModifierTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheHitWithStateChange() {
        // Verify that a state change outside of the cache block does not
        // require the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val size = 200
        rule.setContent {
            var rectColor by remember { mutableStateOf(Color.Blue) }
            AtLeastSize(
                size = size,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    val drawSize = this.size
                    val path = Path().apply {
                        lineTo(drawSize.width / 2f, 0f)
                        lineTo(drawSize.width / 2f, drawSize.height)
                        lineTo(0f, drawSize.height)
                        close()
                    }
                    cacheBuildCount++
                    onDraw {
                        drawRect(rectColor)
                        drawPath(path, Color.Red)
                    }
                }.clickable {
                    if (rectColor == Color.Blue) {
                        rectColor = Color.Green
                    } else {
                        rectColor = Color.Blue
                    }
                }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 - 2))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 - 2))

                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, height - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was re-used and only built once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 - 1))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 - 2))

                assertEquals(Color.Green.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Green.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Green.toArgb(), getPixel(width / 2 + 1, height - 2))
                assertEquals(Color.Green.toArgb(), getPixel(width - 2, height - 2))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheInvalidatedAfterStateChange() {
        // Verify that a state change within the cache block does
        // require the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val size = 200

        rule.setContent {
            var pathFillBounds by remember { mutableStateOf(false) }
            AtLeastSize(
                size = size,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    val pathSize = if (pathFillBounds) this.size else this.size / 2f
                    val path = Path().apply {
                        lineTo(pathSize.width, 0f)
                        lineTo(pathSize.width, pathSize.height)
                        lineTo(0f, pathSize.height)
                        close()
                    }
                    cacheBuildCount++
                    onDraw {
                        drawRect(Color.Red)
                        drawPath(path, Color.Blue)
                    }
                }.clickable {
                    pathFillBounds = !pathFillBounds
                }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 - 2, height / 2 - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(1, height / 2 - 1))

                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, height / 2 - 1))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 + 1, height / 2 - 2))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 2, height / 2 + 1))
                assertEquals(Color.Red.toArgb(), getPixel(1, height / 2 + 1))

                assertEquals(Color.Red.toArgb(), getPixel(1, height - 2))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            assertEquals(2, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(1, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(size - 2, 1))
                assertEquals(Color.Blue.toArgb(), getPixel(size - 2, size - 2))
                assertEquals(Color.Blue.toArgb(), getPixel(1, size - 2))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testCacheInvalidatedAfterSizeChange() {
        // Verify that a size change does cause the cache block to be invalidated
        val testTag = "testTag"
        var cacheBuildCount = 0
        val startSize = 200
        val endSize = 400
        rule.setContent {
            var size by remember { mutableStateOf(startSize) }
            AtLeastSize(
                size = size,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    val drawSize = this.size
                    val path = Path().apply {
                        lineTo(drawSize.width, 0f)
                        lineTo(drawSize.height, drawSize.height)
                        lineTo(0f, drawSize.height)
                        close()
                    }
                    cacheBuildCount++
                    onDraw {
                        drawPath(path, Color.Red)
                    }
                }.clickable {
                    if (size == startSize) {
                        size = endSize
                    } else {
                        size = startSize
                    }
                }
            ) { }
        }

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was created only once
            assertEquals(1, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(startSize, this.width)
                assertEquals(startSize, this.height)
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
            performClick()
        }

        rule.waitForIdle()

        rule.onNodeWithTag(testTag).apply {
            // Verify that the path was re-used and only built once
            assertEquals(2, cacheBuildCount)
            captureToBitmap().apply {
                assertEquals(endSize, this.width)
                assertEquals(endSize, this.height)
                assertEquals(Color.Red.toArgb(), getPixel(1, 1))
                assertEquals(Color.Red.toArgb(), getPixel(width - 2, height - 2))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheContentDrawnImplicitly() {
        // Verify that drawContent is invoked even if it is not explicitly called within
        // the implementation of the callback provided in the onDraw method
        // in Modifier.drawWithCache
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    onDraw {
                        drawRect(Color.Red, size = Size(size.width / 2, size.height))
                    }
                }.background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(0, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Blue.toArgb(), getPixel(0, height - 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheOverContent() {
        // Verify that drawContent is not invoked implicitly if it is explicitly called within
        // the implementation of the callback provided in the onDraw method
        // in Modifier.drawWithCache. That is the red rectangle is drawn above the contents
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(Color.Red, size = Size(size.width / 2, size.height))
                    }
                }.background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Blue.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Blue.toArgb(), getPixel(width / 2 + 1, height - 1))

                assertEquals(Color.Red.toArgb(), getPixel(0, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 1, 0))
                assertEquals(Color.Red.toArgb(), getPixel(width / 2 - 1, height - 1))
                assertEquals(Color.Red.toArgb(), getPixel(0, height - 1))
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testDrawWithCacheBlendsContent() {
        // Verify that the drawing commands of drawContent are blended against the green
        // rectangle with the specified BlendMode
        val testTag = "testTag"
        val testSize = 200
        rule.setContent {
            AtLeastSize(
                size = testSize,
                modifier = Modifier.testTag(testTag).drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(Color.Green, blendMode = BlendMode.Plus)
                    }
                }.background(Color.Blue)
            )
        }

        rule.onNodeWithTag(testTag).apply {
            captureToBitmap().apply {
                assertEquals(Color.Cyan.toArgb(), getPixel(0, 0))
                assertEquals(Color.Cyan.toArgb(), getPixel(width - 1, 0))
                assertEquals(Color.Cyan.toArgb(), getPixel(width - 1, height - 1))
                assertEquals(Color.Cyan.toArgb(), getPixel(0, height - 1))
            }
        }
    }
}