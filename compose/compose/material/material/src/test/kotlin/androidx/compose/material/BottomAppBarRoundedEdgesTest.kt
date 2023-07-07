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
package androidx.compose.material

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Test for [calculateRoundedEdgeIntercept] used in [BottomAppBar] to round the edges of a circular
 * FAB cutout.
 *
 * In each scenario, we start with a circle with radius 5, origin 0,0 - coordinates returned are
 * always relative to the center of the circle.
 *
 * See material/src/test/bottom_app_bar_rounded_edges.png for what these calculated points
 * represent. This is generated from material/src/test/bottom_app_bar_rounded_edges_graph.py
 *
 * Note: on Android y = 0 is at the top of the screen, so positive values are lower down - as a
 * result all the y values in this test are inverted from the graph representation.
 */
@RunWith(JUnit4::class)
class BottomAppBarRoundedEdgesTest {
    companion object {
        const val RADIUS = 5f
    }

    @Test
    fun bottomAppBar_roundedEdgeIntercept_appBarAlignedWithCutoutCenter() {
        val verticalOffset = 0f

        val controlPoint = calculateCutoutCircleYIntercept(RADIUS, verticalOffset) - 1f

        val (calculatedX, calculatedY) =
            calculateRoundedEdgeIntercept(controlPoint, verticalOffset, RADIUS)

        val expectedX = -4.16f
        val expectedY = 2.76f

        assertThat(calculatedX).isWithin(0.01f).of(expectedX)
        assertThat(calculatedY).isWithin(0.01f).of(expectedY)
    }

    @Test
    fun bottomAppBar_roundedEdgeIntercept_appBarAboveCutoutCenter_controlPointOutsideRADIUS() {
        // The top edge of the app bar is 2.5 above the circle's vertical center
        val verticalOffset = -2.5f

        val controlPoint = calculateCutoutCircleYIntercept(RADIUS, verticalOffset) - 1f

        val (calculatedX, calculatedY) =
            calculateRoundedEdgeIntercept(controlPoint, verticalOffset, RADIUS)

        val expectedX = -4.96f
        val expectedY = 0.59f

        assertThat(calculatedX).isWithin(0.01f).of(expectedX)
        assertThat(calculatedY).isWithin(0.01f).of(expectedY)
    }

    @Test
    fun bottomAppBar_roundedEdgeIntercept_appBarAboveCutoutCenter_controlPointInsideRADIUS() {
        // The top edge of the app bar is 4.5 above the circle's vertical center
        val verticalOffset = -4.5f

        val controlPoint = calculateCutoutCircleYIntercept(RADIUS, verticalOffset) - 1f

        val (calculatedX, calculatedY) =
            calculateRoundedEdgeIntercept(controlPoint, verticalOffset, RADIUS)

        val expectedX = -4.33f
        val expectedY = -2.49f

        assertThat(calculatedX).isWithin(0.01f).of(expectedX)
        assertThat(calculatedY).isWithin(0.01f).of(expectedY)
    }

    @Test
    fun bottomAppBar_roundedEdgeIntercept_appBarBelowCutoutCenter() {
        // The top edge of the app bar is 2.5 below the circle's vertical center
        val verticalOffset = 2.5f

        val controlPoint = calculateCutoutCircleYIntercept(RADIUS, verticalOffset) - 1f

        val (calculatedX, calculatedY) =
            calculateRoundedEdgeIntercept(controlPoint, verticalOffset, RADIUS)

        val expectedX = -2.72f
        val expectedY = 4.19f

        assertThat(calculatedX).isWithin(0.01f).of(expectedX)
        assertThat(calculatedY).isWithin(0.01f).of(expectedY)
    }
}
