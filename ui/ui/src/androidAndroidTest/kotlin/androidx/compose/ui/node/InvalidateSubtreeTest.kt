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
package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class InvalidateSubtreeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun invalidateSubtreeNoLayers() {
        lateinit var invalidate: () -> Unit
        val counter1 = LayoutAndDrawCounter()
        val counter2 = LayoutAndDrawCounter()
        val counter3 = LayoutAndDrawCounter()
        val captureInvalidate = CaptureInvalidateCounter { node ->
            invalidate = { node.invalidateSubtree() }
        }
        rule.setContent {
            Box(counter1) {
                Box(counter2 then captureInvalidate) {
                    Box(counter3.size(10.dp))
                }
            }
        }
        rule.waitForIdle()
        assertThat(counter1.drawCount).isEqualTo(1)
        assertThat(counter1.measureCount).isEqualTo(1)
        assertThat(counter1.placeCount).isEqualTo(1)
        assertThat(counter2.drawCount).isEqualTo(1)
        assertThat(counter2.measureCount).isEqualTo(1)
        assertThat(counter2.placeCount).isEqualTo(1)
        assertThat(counter3.drawCount).isEqualTo(1)
        assertThat(counter3.measureCount).isEqualTo(1)
        assertThat(counter3.placeCount).isEqualTo(1)

        rule.runOnUiThread {
            invalidate()
        }
        rule.waitForIdle()

        // There isn't a layer that can be invalidated, so we draw this twice also
        assertThat(counter1.drawCount).isEqualTo(2)
        assertThat(counter1.measureCount).isEqualTo(1)
        assertThat(counter1.placeCount).isEqualTo(1)
        assertThat(counter2.drawCount).isEqualTo(2)
        assertThat(counter2.measureCount).isEqualTo(2)
        assertThat(counter2.placeCount).isEqualTo(2)
        assertThat(counter3.drawCount).isEqualTo(2)
        assertThat(counter3.measureCount).isEqualTo(2)
        assertThat(counter3.placeCount).isEqualTo(2)
    }

    @Test
    fun invalidateSubtreeWithLayers() {
        lateinit var invalidate: () -> Unit
        val counter1 = LayoutAndDrawCounter()
        val counter2 = LayoutAndDrawCounter()
        val counter3 = LayoutAndDrawCounter()
        val counter4 = LayoutAndDrawCounter()
        val captureInvalidate = CaptureInvalidateCounter { node ->
            invalidate = { node.invalidateSubtree() }
        }
        rule.setContent {
            Box(Modifier.graphicsLayer {} then counter1.graphicsLayer { }) {
                Box(Modifier.graphicsLayer { } then
                    counter2 then captureInvalidate.graphicsLayer { } then counter3
                ) {
                    Box(counter4.size(10.dp))
                }
            }
        }
        rule.waitForIdle()
        assertThat(counter1.drawCount).isEqualTo(1)
        assertThat(counter2.drawCount).isEqualTo(1)
        assertThat(counter3.drawCount).isEqualTo(1)
        assertThat(counter4.drawCount).isEqualTo(1)

        rule.runOnUiThread {
            invalidate()
        }
        rule.waitForIdle()

        assertThat(counter1.drawCount).isEqualTo(1)
        assertThat(counter2.drawCount).isEqualTo(2)
        assertThat(counter3.drawCount).isEqualTo(2)
        assertThat(counter4.drawCount).isEqualTo(2)
    }

    private class LayoutAndDrawCounter : LayoutModifier, DrawModifier {
        var measureCount = 0
        var placeCount = 0
        var drawCount = 0
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            measureCount++
            val p = measurable.measure(constraints)
            return layout(p.width, p.height) {
                placeCount++
                p.place(0, 0)
            }
        }

        override fun ContentDrawScope.draw() {
            drawCount++
            drawContent()
        }
    }

    private class CaptureInvalidateCounter(
        private val onCreate: (node: Modifier.Node) -> Unit
    ) : ModifierNodeElement<Modifier.Node>() {
        override fun create() = object : Modifier.Node() {}
            .apply<Modifier.Node>(onCreate)

        override fun update(node: Modifier.Node) = node

        override fun InspectorInfo.inspectableProperties() {
            name = "Invalidate Subtree Modifier.Node"
        }

        override fun hashCode() = 0
        override fun equals(other: Any?) = (other === this)
    }
}