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

package androidx.compose.ui.semantics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.MockOwner
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.zIndex
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class SemanticsSortTest {

    @Test // regression test for b/207477257
    fun compareDoesNotViolateComparatorContract() {
        val root = LayoutNode(0, 0, 720, 1080)
        repeat(32) { index ->
            val child = if (index % 2 == 0) {
                LayoutNode(0, 0, 0, 0)
            } else {
                val offset = if (index == 1 || index == 31) 100 else 0
                LayoutNode(0, 0 - offset, 720, 30 - offset).also {
                    it.insertAt(0, LayoutNode(0, 0, 100, 100, Modifier.semantics { }))
                }
            }
            root.insertAt(index, child)
        }

        root.attach(MockOwner())
        root.findOneLayerOfSemanticsWrappersSortedByBounds()

        // expect - no crash happened
    }

    @Test
    fun sortedByZOrderIfHasSameBounds() {
        val root = LayoutNode(0, 0, 100, 100)
        repeat(5) { index ->
            root.insertAt(
                index,
                LayoutNode(
                    0, 0, 100, 100,
                    Modifier
                        .semantics { set(LayoutNodeIndex, index) }
                        .zIndex((index * 3 % 5).toFloat())
                )
            )
        }
        root.attach(MockOwner())
        root.remeasure()
        root.replace()
        val result = root.findOneLayerOfSemanticsWrappersSortedByBounds()

        assertThat(result[0].layoutNodeIndex()).isEqualTo(3)
        assertThat(result[1].layoutNodeIndex()).isEqualTo(1)
        assertThat(result[2].layoutNodeIndex()).isEqualTo(4)
        assertThat(result[3].layoutNodeIndex()).isEqualTo(2)
        assertThat(result[4].layoutNodeIndex()).isEqualTo(0)
    }

    private val LayoutNodeIndex = SemanticsPropertyKey<Int>("LayoutNodeIndex")

    private fun SemanticsModifierNode.layoutNodeIndex(): Int {
        return semanticsConfiguration[LayoutNodeIndex]
    }
}
