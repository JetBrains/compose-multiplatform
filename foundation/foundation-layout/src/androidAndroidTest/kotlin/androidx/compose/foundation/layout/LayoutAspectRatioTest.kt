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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Ref
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class LayoutAspectRatioTest : LayoutTest() {
    @Test
    fun testAspectRatioModifier_intrinsicDimensions() = with(density) {
        testIntrinsics(
            @Composable {
                Container(modifier = Modifier.aspectRatio(2f), width = 30.dp, height = 40.dp) { }
            }
        ) { minIntrinsicWidth, minIntrinsicHeight, maxIntrinsicWidth, maxIntrinsicHeight ->
            assertEquals(40, minIntrinsicWidth(20))
            assertEquals(40, maxIntrinsicWidth(20))
            assertEquals(20, minIntrinsicHeight(40))
            assertEquals(20, maxIntrinsicHeight(40))

            assertEquals(30.dp.toIntPx(), minIntrinsicWidth(Constraints.Infinity))
            assertEquals(30.dp.toIntPx(), maxIntrinsicWidth(Constraints.Infinity))
            assertEquals(40.dp.toIntPx(), minIntrinsicHeight(Constraints.Infinity))
            assertEquals(40.dp.toIntPx(), maxIntrinsicHeight(Constraints.Infinity))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAspectRatioModifier_zeroRatio() {
        Modifier.aspectRatio(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAspectRatioModifier_negativeRatio() {
        Modifier.aspectRatio(-2f)
    }

    @Test
    fun testAspectRatio_sizesCorrectly() {
        assertEquals(IntSize(30, 30), getSize(1f, Constraints(maxWidth = 30)))
        assertEquals(IntSize(30, 15), getSize(2f, Constraints(maxWidth = 30)))
        assertEquals(
            IntSize(10, 10),
            getSize(1f, Constraints(maxWidth = 30, maxHeight = 10))
        )
        assertEquals(
            IntSize(20, 10),
            getSize(2f, Constraints(maxWidth = 30, maxHeight = 10))
        )
        assertEquals(
            IntSize(10, 5),
            getSize(2f, Constraints(minWidth = 10, minHeight = 5))
        )
        assertEquals(
            IntSize(20, 10),
            getSize(2f, Constraints(minWidth = 5, minHeight = 10))
        )
    }

    private fun getSize(aspectRatio: Float, childContraints: Constraints): IntSize {
        val positionedLatch = CountDownLatch(1)
        val size = Ref<IntSize>()
        val position = Ref<Offset>()
        show {
            Layout(
                @Composable {
                    Container(
                        Modifier
                            .aspectRatio(aspectRatio)
                            .then(Modifier.saveLayoutInfo(size, position, positionedLatch))
                    ) {
                    }
                }
            ) { measurables, incomingConstraints ->
                require(measurables.isNotEmpty())
                val placeable = measurables.first().measure(childContraints)
                layout(incomingConstraints.maxWidth, incomingConstraints.maxHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        return size.value!!
    }
}