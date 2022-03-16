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

import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MeasureAndLayoutDelegateTest {

    private val DifferentSize = 50
    private val DifferentSize2 = 30

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun requiresMeasureWhenJustCreated() {
        val root = root {
            add(node())
        }

        createDelegate(root, firstMeasureCompleted = false)

        assertMeasureRequired(root)
        assertMeasureRequired(root.first)
    }

    @Test
    fun measureNotRequiredAfterFirstMeasure() {
        val root = root {
            add(node())
        }

        createDelegate(root)

        assertMeasuredAndLaidOut(root)
        assertMeasuredAndLaidOut(root.first)
    }

    @Test
    fun relayoutNotRequiredAfterFirstMeasure() {
        val root = root {
            add(node())
        }

        createDelegate(root)

        assertMeasuredAndLaidOut(root)
        assertMeasuredAndLaidOut(root.first)
    }

    @Test
    fun measuredAndLaidOutAfterFirstMeasureAndLayout() {
        val root = root {
            add(node())
        }

        assertRemeasured(root) {
            assertRemeasured(root.first) {
                createDelegate(root)
            }
        }
    }

    @Test
    fun rootNodeIsPlacedWhenAttached() {
        val root = root {}
        createDelegate(root, firstMeasureCompleted = false)
        val owner = root.owner!!

        assertThat(root.isPlaced).isTrue()

        root.detach()

        assertThat(root.isPlaced).isFalse()

        root.attach(owner)

        assertThat(root.isPlaced).isTrue()
    }

    // remeasure request:

    @Test
    fun childRemeasureRequest_remeasureRequired() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        delegate.requestRemeasure(root.first)
        assertMeasureRequired(root.first)
    }

    @Test
    fun childRemeasureRequest_childRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childMeasuredInLayoutBlockRemeasureRequest_childRemeasured() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureWithTheSameResult_parentNotRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureWithDifferentResult_parentRemeasured() {
        val root = root {
            wrapChildren = true
            add(
                node {
                    size = DifferentSize
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertRemeasured(root) {
                root.first.size = DifferentSize2
                delegate.requestRemeasure(root.first)
                assertThat(delegate.measureAndLayout()).isTrue()
            }
        }
    }

    @Test
    fun childRemeasureInLayoutBlockWithTheSameResult_parentNotRemeasured() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureInLayoutBlockWithDifferentResult_parentNotRemeasured() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            root.first.size = DifferentSize
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureRequest_childRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childMeasuredInLayoutBlockRelayoutRequest_childRelaidOut() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureWithTheSameResult_parentNotRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureInLayoutBlockWithTheSameResult_parentNotRelaidOut() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureInLayoutBlockWithDifferentResult_parentRelaidOut() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root) {
            root.first.size = DifferentSize
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun rootRemeasureRequest_childNotAffected() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertNotRemeasured(root.first) {
                assertNotRelaidOut(root.first) {
                    delegate.requestRemeasure(root)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun parentRemeasureRequest_childNotAffected() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertNotRemeasured(root.first.first) {
                assertNotRelaidOut(root.first.first) {
                    delegate.requestRemeasure(root.first)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    // relayout request:

    @Test
    fun childRelayoutRequest_childRelayoutRequired() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        delegate.requestRelayout(root.first)
        assertLayoutRequired(root.first)
    }

    @Test
    fun childRelayoutRequest_childRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRelayoutRequest_childNotRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root.first) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRelayoutRequest_parentNotRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRelayoutRequest_parentNotRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childMeasuredInLayoutBlockRelayoutRequest_parentNotRemeasured() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childMeasuredInLayoutBlockRelayoutRequest_parentNotRelaidOut() {
        val root = root {
            add(node())
            measureInLayoutBlock()
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun rootRelayoutRequest_childNotAffected() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root) {
            assertNotRelaidOut(root.first) {
                delegate.requestRelayout(root)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun parentRelayoutRequest_childNotAffected() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            assertNotRelaidOut(root.first.first) {
                delegate.requestRelayout(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    // request twice

    @Test
    fun childRemeasureRequestedTwice_childRemeasuredOnce() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            delegate.requestRemeasure(root.first)
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureRequestedTwice_childRelaidOutOnce() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRemeasure(root.first)
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureAndRelayoutRequested_childRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            delegate.requestRemeasure(root.first)
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRemeasureAndRelayoutRequested_childRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRemeasure(root.first)
            delegate.requestRelayout(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRelayoutAndRemeasureRequested_childRemeasured() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            delegate.requestRelayout(root.first)
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun childRelayoutAndRemeasureRequested_childRelaidOut() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            delegate.requestRelayout(root.first)
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    // Siblings

    @Test
    fun firstChildRemeasureRequest_onlyFirstChildRemeasured() {
        val root = root {
            add(node())
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertNotRemeasured(root.second) {
                delegate.requestRemeasure(root.first)
                delegate.measureAndLayout()
            }
        }
    }

    @Test
    fun firstChildRelayoutRequest_onlyFirstChildRelaid() {
        val root = root {
            add(node())
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            assertNotRelaidOut(root.second) {
                delegate.requestRelayout(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun bothChildrenRemeasureRequest_bothRemeasured() {
        val root = root {
            add(node())
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertRemeasured(root.second) {
                delegate.requestRemeasure(root.first)
                delegate.requestRemeasure(root.second)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun bothChildrenRelayoutRequest_bothRelaidOut() {
        val root = root {
            add(node())
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root.first) {
            assertRelaidOut(root.second) {
                delegate.requestRelayout(root.first)
                delegate.requestRelayout(root.second)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun oneChildRelayoutRequestAnotherRemeasure() {
        val root = root {
            add(node())
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertNotRemeasured(root.second) {
                assertRelaidOut(root.second) {
                    delegate.requestRemeasure(root.first)
                    delegate.requestRelayout(root.second)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    // different levels

    @Test
    fun remeasureTwoNodesOnDifferentLayers_othersAreNotAffected() {
        val root = root {
            add(node())
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            assertRemeasured(root.first) {
                assertNotRemeasured(root.second) {
                    assertRemeasured(root.second.first) {
                        delegate.requestRemeasure(root.first)
                        delegate.requestRemeasure(root.second.first)
                        assertThat(delegate.measureAndLayout()).isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun changeSizeOfTheLeaf_remeasuresUpToTheFixedSizeParent() {
        val root = root {
            wrapChildren = true
            add(
                node {
                    size = DifferentSize
                    add(
                        node {
                            wrapChildren = true
                            add(
                                node {
                                    size = DifferentSize
                                }
                            )
                        }
                    )
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            assertRemeasured(root.first) {
                assertRemeasured(root.first.first) {
                    val leaf = root.first.first.first
                    assertRemeasured(leaf) {
                        leaf.size = DifferentSize2
                        delegate.requestRemeasure(leaf)
                        assertThat(delegate.measureAndLayout()).isFalse()
                    }
                }
            }
        }
    }

    @Test
    fun remeasureRequestForItemsOnTheSameLevelButDifferentParents() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            assertNotRemeasured(root.first) {
                assertRemeasured(root.first.first) {
                    assertNotRemeasured(root.second) {
                        assertRemeasured(root.second.first) {
                            delegate.requestRemeasure(root.first.first)
                            delegate.requestRemeasure(root.second.first)
                            assertThat(delegate.measureAndLayout()).isFalse()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun relayoutRequestForItemsOnTheSameLevelButDifferentParents() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            assertNotRelaidOut(root.first) {
                assertRelaidOut(root.first.first) {
                    assertNotRelaidOut(root.second) {
                        assertRelaidOut(root.second.first) {
                            delegate.requestRelayout(root.first.first)
                            delegate.requestRelayout(root.second.first)
                            assertThat(delegate.measureAndLayout()).isFalse()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun relayoutAndRemeasureRequestForItemsOnTheSameLevelButDifferentParents() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRelaidOut(root) {
            assertNotRelaidOut(root.first) {
                assertRemeasured(root.first.first) {
                    assertNotRelaidOut(root.second) {
                        assertRelaidOut(root.second.first) {
                            delegate.requestRemeasure(root.first.first)
                            delegate.requestRelayout(root.second.first)
                            assertThat(delegate.measureAndLayout()).isFalse()
                        }
                    }
                }
            }
        }
    }

    // request during measure

    @Test
    fun requestChildRemeasureDuringMeasure() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertRemeasured(root.first) {
                root.runDuringMeasure {
                    delegate.requestRemeasure(root.first)
                }
                delegate.requestRemeasure(root)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun requestGrandchildRemeasureDuringMeasure() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertNotRemeasured(root.first) {
                assertRemeasured(root.first.first) {
                    root.runDuringMeasure {
                        delegate.requestRemeasure(root.first.first)
                    }
                    delegate.requestRemeasure(root)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun requestChildRelayoutDuringMeasure() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertRelaidOut(root.first) {
                root.runDuringMeasure {
                    delegate.requestRelayout(root.first)
                }
                delegate.requestRemeasure(root)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun requestGrandchildRelayoutDuringMeasure() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertNotRelaidOut(root.first) {
                assertRelaidOut(root.first.first) {
                    root.runDuringMeasure {
                        delegate.requestRelayout(root.first.first)
                    }
                    delegate.requestRemeasure(root)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun requestChildRemeasureDuringParentLayout() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root) {
            assertRemeasured(root.first) {
                root.runDuringLayout {
                    delegate.requestRemeasure(root.first)
                }
                delegate.requestRelayout(root)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun requestGrandchildRemeasureDuringParentLayout() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRelaidOut(root) {
            assertNotRelaidOut(root.first) {
                assertRemeasured(root.first.first) {
                    root.runDuringLayout {
                        delegate.requestRemeasure(root.first.first)
                    }
                    delegate.requestRelayout(root)
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun requestRemeasureForCurrentlyBeingRemeasuredNode() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            root.runDuringMeasure {
                delegate.requestRemeasure(root.first)
            }
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun requestRelayoutForCurrentlyBeingRemeasuredNode() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertRelaidOut(root.first) {
                root.runDuringMeasure {
                    delegate.requestRelayout(root.first)
                }
                delegate.requestRemeasure(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun requestRemeasureForCurrentlyBeingRelayoutNode() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertRelaidOut(root.first, times = 2) {
                root.first.runDuringLayout {
                    delegate.requestRemeasure(root.first)
                }
                delegate.requestRelayout(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    @Test
    fun requestRelayoutForCurrentlyBeingRelayoutNode() {
        val root = root {
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root.first) {
            assertRelaidOut(root.first) {
                root.runDuringLayout {
                    delegate.requestRelayout(root.first)
                }
                delegate.requestRelayout(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    // Updating root constraints

    @Test
    fun changingParentParamsToTheSameValue_noRemeasures() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            assertNotRemeasured(root.first) {
                assertNotRemeasured(root.first.first) {
                    delegate.updateRootConstraints(
                        defaultRootConstraints()
                    )
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun changingParentConstraints_remeasureSubTree() {
        val root = root {
            add(
                node {
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertRemeasured(root.first) {
                assertRemeasured(root.first.first) {
                    delegate.updateRootConstraints(
                        Constraints(maxWidth = DifferentSize, maxHeight = DifferentSize)
                    )
                    assertThat(delegate.measureAndLayout()).isTrue()
                }
            }
        }
    }

    @Test
    fun changingParentConstraints_remeasureOnlyAffectedNodes() {
        val root = root {
            add(
                node {
                    size = DifferentSize2
                    add(node())
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(root.first) {
            assertNotRemeasured(root.first.first) {
                delegate.updateRootConstraints(
                    Constraints(maxWidth = DifferentSize, maxHeight = DifferentSize)
                )
                assertThat(delegate.measureAndLayout()).isTrue()
            }
        }
    }

    // LayoutModifier

    @Test
    fun requestRemeasureTriggersModifierRemeasure() {
        val spyModifier = SpyLayoutModifier()
        val root = root {
            add(
                node {
                    modifier = spyModifier
                }
            )
        }

        val delegate = createDelegate(root)

        assertRemeasured(spyModifier) {
            delegate.requestRemeasure(root.first)
            assertThat(delegate.measureAndLayout()).isFalse()
        }
    }

    @Test
    fun requestRelayoutTriggersModifierRelayout() {
        val spyModifier = SpyLayoutModifier()
        val root = root {
            add(
                node {
                    modifier = spyModifier
                }
            )
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(spyModifier) {
            assertRelaidOut(spyModifier) {
                delegate.requestRelayout(root.first)
                assertThat(delegate.measureAndLayout()).isFalse()
            }
        }
    }

    // Relayout depending on the measured child
    // Illustrates the case when we run layoutChildren() on the parent node, but some of its
    // children are not yet measured even if they are supposed to be measured in the measure
    // block of our parent.
    //
    // Example:
    // val child = Layout(...)
    // Layout(child) { measuruables, constraints ->
    //    val placeable = measurables.first().measure(constraints)
    //    layout(placeable.width, placeable.height) {
    //       placeable.place(0, 0)
    //    }
    // }
    // Then some changes scheduled remeasure for child and relayout for parent.
    // During the measureAndLayout() we will start with the parent as it has lower depth.
    // Inside the layout block we will call placeable.width which is currently dirty as the child
    // was scheduled to remeasure.

    @Test
    fun relayoutDependingOnRemeasuredChild() {
        val root = root {
            // this node will be measured in the measuring block
            add(node())
        }

        val delegate = createDelegate(root)

        assertNotRemeasured(root) {
            assertRelaidOut(root) {
                assertRemeasured(root.first) {
                    delegate.requestRemeasure(root.first)
                    delegate.requestRelayout(root)
                    root.runDuringLayout {
                        // this means the root.first will be measured before laying out the root
                        assertThat(root.first.layoutPending).isTrue()
                    }
                    assertThat(delegate.measureAndLayout()).isFalse()
                }
            }
        }
    }

    @Test
    fun relayoutDependingOnRemeasuredChild_parentRemeasuredBecauseOfChangedSize() {
        val root = root {
            wrapChildren = true
            // this node will be measured in the measuring block
            add(node())
        }

        val delegate = createDelegate(root)

        assertRemeasured(root) {
            assertRemeasured(root.first) {
                root.first.size = DifferentSize
                delegate.requestRemeasure(root.first)
                delegate.requestRelayout(root)
                assertThat(delegate.measureAndLayout()).isTrue()
            }
        }
    }

    @Test
    fun hasRelayoutNodes() {
        val root = root {
            add(node())
        }
        val delegate = createDelegate(root)
        assertThat(delegate.hasPendingMeasureOrLayout).isFalse()
        delegate.requestRemeasure(root.first)
        assertThat(delegate.hasPendingMeasureOrLayout).isTrue()
        delegate.measureAndLayout()
        assertThat(delegate.hasPendingMeasureOrLayout).isFalse()
    }

    @Test
    fun theWholeSubtreeIsNotPlacedWhenParentWasntPlaced() {
        val root = root {
            add(
                node {
                    add(
                        node {
                            add(node())
                        }
                    )
                }
            )
        }

        val delegate = createDelegate(root)

        root.shouldPlaceChildren = false
        delegate.requestRelayout(root)
        delegate.measureAndLayout()

        assertThat(root.isPlaced).isTrue()
        assertThat(root.first.isPlaced).isFalse()
        assertThat(root.first.first.isPlaced).isFalse()
        assertThat(root.first.first.isPlaced).isFalse()
    }

    @Test
    fun remeasuringNodeSecondTimeWithinTheSameIteration() {
        lateinit var node1: LayoutNode
        lateinit var node2: LayoutNode
        lateinit var node3: LayoutNode
        lateinit var node4: LayoutNode
        lateinit var node5: LayoutNode
        val root = root {
            size = 100
            add(node {
                node1 = this
                size = 50
                add(node {
                    node2 = this
                    add(node { node3 = this })
                })
                add(node { node4 = this })
            })
            add(node { node5 = this })
        }

        val delegate = createDelegate(root)

        delegate.requestRemeasure(root)
        // we change the root size so now node1 and node5 will be remeasured for the new size
        root.size = 50
        // we also want to remeasure node3. as node2 is not scheduled for remeasure node3 will
        // be remeasured via owner.forceMeasureTheSubtree() logic.
        delegate.requestRemeasure(node3)
        // we also want node5 to synchronously request remeasure for already measured node1
        node5.runDuringMeasure {
            delegate.requestRemeasure(node1)
        }
        node2.toString()
        node4.toString()
        // this was crashing and reported as b/208675143
        assertRemeasured(root) {
            assertRemeasured(node1, times = 2) {
                assertNotRemeasured(node2) {
                    assertRemeasured(node3) {
                        assertNotRemeasured(node4) {
                            assertRemeasured(node5) {
                                delegate.measureAndLayout()
                            }
                        }
                    }
                }
            }
        }
    }
}