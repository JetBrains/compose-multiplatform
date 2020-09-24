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

package androidx.compose.ui.layout

import androidx.compose.ui.Measurable
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.unit.Constraints
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class PlacedChildTest {

    @Test
    @OptIn(ExperimentalLayoutNodeApi::class)
    fun remeasureNotPlacedChild() {
        val root = root {
            measureBlocks = UseChildSizeButNotPlace
            add(
                node {
                    wrapChildren = true
                    add(
                        node {
                            size = 10
                        }
                    )
                }
            )
        }

        val delegate = createDelegate(root)

        assertThat(root.height).isEqualTo(10)

        val childWithSize = root.first.first
        childWithSize.size = 20
        childWithSize.requestRemeasure()
        delegate.measureAndLayout()

        assertThat(root.height).isEqualTo(20)
    }
}

@OptIn(ExperimentalLayoutNodeApi::class)
private val UseChildSizeButNotPlace = object : LayoutNode.NoIntrinsicsMeasureBlocks("") {
    override fun measure(
        measureScope: MeasureScope,
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureScope.MeasureResult {
        val placeable = measurables.first().measure(constraints)
        return measureScope.layout(placeable.width, placeable.height) {
            // do not place
        }
    }
}
