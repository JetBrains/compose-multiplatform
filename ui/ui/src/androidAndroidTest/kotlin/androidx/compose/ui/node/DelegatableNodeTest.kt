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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class DelegatableNodeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun visitChildren_noChildren() {
        // Arrange.
        val testNode = object : Modifier.Node() {}
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = modifierElementOf { testNode })
        }

        // Act.
        rule.runOnIdle {
            testNode.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).isEmpty()
    }

    @Test
    fun visitChildWithinCurrentLayoutNode_immediateChild() {
        // Arrange.
        val (node1, node2, node3) = List(3) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .then(modifierElementOf { node2 })
                    .then(modifierElementOf { node3 })
            )
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2)
    }

    @Test
    fun visitChildWithinCurrentLayoutNode_nonContiguousChild() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .otherModifier()
                    .then(modifierElementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2)
    }

    @Test
    fun visitChildrenInOtherLayoutNodes() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(5) { object : Modifier.Node() {} }
        val visitedChildren = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = modifierElementOf { node1 }) {
                Box(modifier = modifierElementOf { node2 }) {
                    Box(modifier = modifierElementOf { node3 })
                }
                Box(modifier = modifierElementOf { node4 }) {
                    Box(modifier = modifierElementOf { node5 })
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node1.visitChildren(Nodes.Any) {
                visitedChildren.add(it)
            }
        }

        // Assert.
        assertThat(visitedChildren).containsExactly(node2, node4).inOrder()
    }

    @Test
    fun visitAncestorWithinCurrentLayoutNode_immediateParent() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .then(modifierElementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node2.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors.first()).isEqualTo(node1)
    }

    @Test
    fun visitAncestorWithinCurrentLayoutNode_nonContiguousAncestor() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .border(10.dp, Color.Red)
                    .then(modifierElementOf { node2 })
            )
        }

        // Act.
        rule.runOnIdle {
            node2.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors).contains(node1)
    }

    @Test
    fun visitAncestorsInOtherLayoutNodes() {
        // Arrange.
        val (node1, node2, node3, node4, node5) = List(5) { object : Modifier.Node() {} }
        val visitedAncestors = mutableListOf<Modifier.Node>()
        rule.setContent {
            Box(modifier = modifierElementOf { node1 }) {
                Box(
                    modifier = Modifier
                        .then(modifierElementOf { node2 })
                        .then(modifierElementOf { node3 })
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .then(modifierElementOf { node4 })
                                .then(modifierElementOf { node5 })
                        )
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle {
            node5.visitAncestors(Nodes.Any) {
                visitedAncestors.add(it)
            }
        }

        // Assert.
        assertThat(visitedAncestors)
            .containsAtLeastElementsIn(arrayOf(node4, node3, node2, node1))
            .inOrder()
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_immediateParent() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .then(modifierElementOf { node2 })
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun nearestAncestorWithinCurrentLayoutNode_nonContiguousAncestor() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(
                modifier = modifierElementOf { node1 }
                    .otherModifier()
                    .then(modifierElementOf { node2 })
            )
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_immediateParentLayoutNode() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(modifier = modifierElementOf { node1 }) {
                Box(modifier = modifierElementOf { node2 })
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    @Test
    fun nearestAncestorInDifferentLayoutNode_nonContiguousParentLayoutNode() {
        // Arrange.
        val (node1, node2) = List(2) { object : Modifier.Node() {} }
        rule.setContent {
            Box(modifier = modifierElementOf { node1 }) {
                Box {
                    Box(modifier = modifierElementOf { node2 })
                }
            }
        }

        // Act.
        val parent = rule.runOnIdle {
            node2.nearestAncestor(Nodes.Any)
        }

        // Assert.
        assertThat(parent).isEqualTo(node1)
    }

    private fun Modifier.otherModifier(): Modifier = this.then(Modifier)
}

@ExperimentalComposeUiApi
internal inline fun <reified T : Modifier.Node> modifierElementOf(
    crossinline create: () -> T,
): Modifier = object : ModifierNodeElement<T>(null, {}) {
    override fun create(): T = create()
    override fun update(node: T): T = node
}