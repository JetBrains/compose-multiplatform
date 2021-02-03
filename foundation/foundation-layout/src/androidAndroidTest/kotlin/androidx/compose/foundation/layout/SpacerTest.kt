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

package androidx.compose.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class SpacerTest : LayoutTest() {

    private val bigConstraints = DpConstraints(
        maxWidth = 5000.dp,
        maxHeight = 5000.dp
    )

    @Test
    fun fixedSpacer_Sizes() {
        var size: IntSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(
                    Modifier.size(width = width, height = height)
                        .onGloballyPositioned { position: LayoutCoordinates ->
                            size = position.size
                            drawLatch.countDown()
                        }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(height.roundToPx())
            Truth.assertThat(size?.width).isEqualTo(width.roundToPx())
        }
    }

    @Test
    fun fixedSpacer_Sizes_WithSmallerContainer() {
        var size: IntSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Box {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(
                        Modifier.size(width = width, height = height)
                            .onGloballyPositioned { position: LayoutCoordinates ->
                                size = position.size
                                drawLatch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(containerHeight.roundToPx())
            Truth.assertThat(size?.width).isEqualTo(containerWidth.roundToPx())
        }
    }

    @Test
    fun widthSpacer_Sizes() {
        var size: IntSize? = null
        val width = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(
                    Modifier.width(width).onGloballyPositioned { position ->
                        size = position.size
                        drawLatch.countDown()
                    }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(0)
            Truth.assertThat(size?.width).isEqualTo(width.roundToPx())
        }
    }

    @Test
    fun widthSpacer_Sizes_WithSmallerContainer() {
        var size: IntSize? = null
        val width = 40.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Box {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(
                        Modifier.width(width)
                            .onGloballyPositioned { position: LayoutCoordinates ->
                                size = position.size
                                drawLatch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(0)
            Truth.assertThat(size?.width).isEqualTo(containerWidth.roundToPx())
        }
    }

    @Test
    fun heightSpacer_Sizes() {
        var size: IntSize? = null
        val height = 7.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(
                    Modifier.height(height)
                        .onGloballyPositioned { position: LayoutCoordinates ->
                            size = position.size
                            drawLatch.countDown()
                        }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(height.roundToPx())
            Truth.assertThat(size?.width).isEqualTo(0)
        }
    }

    @Test
    fun heightSpacer_Sizes_WithSmallerContainer() {
        var size: IntSize? = null
        val height = 23.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Box {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(
                        Modifier.height(height)
                            .onGloballyPositioned { position: LayoutCoordinates ->
                                size = position.size
                                drawLatch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(containerHeight.roundToPx())
            Truth.assertThat(size?.width).isEqualTo(0)
        }
    }
}