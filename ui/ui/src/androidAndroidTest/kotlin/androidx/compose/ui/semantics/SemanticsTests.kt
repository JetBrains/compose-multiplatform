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

package androidx.compose.ui.semantics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.test.filters.MediumTest
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assert
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertLabelEquals
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.onAllNodesWithLabel
import androidx.ui.test.onAllNodesWithText
import androidx.ui.test.onNodeWithLabel
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import kotlin.math.max

@MediumTest
@RunWith(AndroidJUnit4::class)
class SemanticsTests {
    private val TestTag = "semantics-test-tag"

    @get:Rule
    val rule = createComposeRule()

    private fun executeUpdateBlocking(updateFunction: () -> Unit) {
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            updateFunction()
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun unchangedSemanticsDoesNotCauseRelayout() {
        val layoutCounter = Counter(0)
        val recomposeForcer = mutableStateOf(0)
        rule.setContent {
            recomposeForcer.value
            CountingLayout(Modifier.semantics { accessibilityLabel = "label" }, layoutCounter)
        }

        rule.runOnIdle { assertEquals(1, layoutCounter.count) }

        rule.runOnIdle { recomposeForcer.value++ }

        rule.runOnIdle { assertEquals(1, layoutCounter.count) }
    }

    @Test
    fun depthFirstLabelConcat() {
        val root = "root"
        val child1 = "child1"
        val grandchild1 = "grandchild1"
        val grandchild2 = "grandchild2"
        val child2 = "grandchild2"
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag)
                    .semantics(mergeAllDescendants = true) { accessibilityLabel = root }
            ) {
                SimpleTestLayout(Modifier.semantics { accessibilityLabel = child1 }) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = grandchild1 }) { }
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = grandchild2 }) { }
                }
                SimpleTestLayout(Modifier.semantics { accessibilityLabel = child2 }) { }
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(
            "$root, $child1, $grandchild1, $grandchild2, $child2"
        )
    }

    @Test
    fun nestedMergedSubtree() {
        val tag1 = "tag1"
        val tag2 = "tag2"
        val label1 = "foo"
        val label2 = "bar"
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true) {}.testTag(tag1)) {
                SimpleTestLayout(Modifier.semantics { accessibilityLabel = label1 }) { }
                SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true) {}.testTag(tag2)) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label2 }) { }
                }
            }
        }

        rule.onNodeWithTag(tag1).assertLabelEquals(label1)
        rule.onNodeWithTag(tag2).assertLabelEquals(label2)
    }

    @Test
    fun removingMergedSubtree_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true) {}.testTag(TestTag)) {
                if (showSubtree.value) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                }
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(label)

        rule.runOnIdle { showSubtree.value = false }

        rule.onNodeWithTag(TestTag)
            .assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)

        rule.onAllNodesWithText(label).assertCountEquals(0)
    }

    @Test
    fun addingNewMergedNode_updatesSemantics() {
        val label = "foo"
        val value = "bar"
        val showNewNode = mutableStateOf(false)
        rule.setContent {
            SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true) {}.testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                if (showNewNode.value) {
                    SimpleTestLayout(Modifier.semantics { accessibilityValue = value }) { }
                }
            }
        }

        rule.onNodeWithTag(TestTag)
            .assertLabelEquals(label)
            .assertDoesNotHaveProperty(SemanticsProperties.AccessibilityValue)

        rule.runOnIdle { showNewNode.value = true }

        rule.onNodeWithTag(TestTag)
            .assertLabelEquals(label)
            .assertValueEquals(value)
    }

    @Test
    fun removingSubtreeWithoutSemanticsAsTopNode_updatesSemantics() {
        val label = "foo"
        val showSubtree = mutableStateOf(true)
        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                if (showSubtree.value) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                }
            }
        }

        rule.onAllNodesWithLabel(label).assertCountEquals(1)

        rule.runOnIdle {
            showSubtree.value = false
        }

        rule.onAllNodesWithLabel(label).assertCountEquals(0)
    }

    @Test
    fun changingStackedSemanticsComponent_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)
        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                }
            ) {}
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingStackedSemanticsComponent_notTopMost_updatesSemantics() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(Modifier.testTag("don't care")) {
                SimpleTestLayout(
                    Modifier.testTag(TestTag).semantics {
                        accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                    }
                ) {}
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowStackedLayoutNodes_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout {
                SimpleTestLayout {
                    SimpleTestLayout(
                        Modifier.testTag(TestTag).semantics {
                            accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                        }
                    ) {}
                }
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_belowNodeMergedThroughBoundary_updatesCorrectly() {
        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag).semantics(mergeAllDescendants = true) {}) {
                SimpleTestLayout(
                    Modifier.semantics {
                        accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                    }
                ) {}
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun mergeAllDescendants_doesNotCrossLayoutNodesUpward() {
        val label = "label"
        rule.setContent {
            SimpleTestLayout(Modifier.testTag(TestTag)) {
                SimpleTestLayout(Modifier.semantics(mergeAllDescendants = true) {}) {
                    SimpleTestLayout(Modifier.semantics { accessibilityLabel = label }) { }
                }
            }
        }

        rule.onNodeWithTag(TestTag)
            .assertDoesNotHaveProperty(SemanticsProperties.AccessibilityLabel)
        rule.onNodeWithLabel(label) // assert exists
    }

    @Test
    fun updateToNodeWithMultipleBoundaryChildren_updatesCorrectly() {
        // This test reproduced a bug that caused a ConcurrentModificationException when
        // detaching SemanticsNodes

        val beforeLabel = "before"
        val afterLabel = "after"
        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                }
            ) {
                SimpleTestLayout(Modifier.semantics { }) { }
                SimpleTestLayout(Modifier.semantics { }) { }
            }
        }

        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)

        rule.runOnIdle { isAfter.value = true }

        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
    }

    @Test
    fun changingSemantics_doesNotReplaceNodesBelow() {
        // Regression test for b/148606417
        var nodeCount = 0
        val beforeLabel = "before"
        val afterLabel = "after"

        // Do different things in an attempt to defeat a sufficiently clever compiler
        val beforeAction = { println("this never gets called") }
        val afterAction = { println("neither does this") }

        val isAfter = mutableStateOf(false)

        rule.setContent {
            SimpleTestLayout(
                Modifier.testTag(TestTag).semantics {
                    accessibilityLabel = if (isAfter.value) afterLabel else beforeLabel
                    onClick(
                        action = {
                            if (isAfter.value) afterAction() else beforeAction()
                            return@onClick true
                        }
                    )
                }
            ) {
                SimpleTestLayout {
                    remember { nodeCount++ }
                }
            }
        }

        // This isn't the important part, just makes sure everything is behaving as expected
        rule.onNodeWithTag(TestTag).assertLabelEquals(beforeLabel)
        assertThat(nodeCount).isEqualTo(1)

        rule.runOnIdle { isAfter.value = true }

        // Make sure everything is still behaving as expected
        rule.onNodeWithTag(TestTag).assertLabelEquals(afterLabel)
        // This is the important part: make sure we didn't replace the identity due to unwanted
        // pivotal properties
        assertThat(nodeCount).isEqualTo(1)
    }
}

private fun SemanticsNodeInteraction.assertDoesNotHaveProperty(property: SemanticsPropertyKey<*>) {
    assert(SemanticsMatcher.keyNotDefined(property))
}

// Falsely mark the layout counter stable to avoid influencing recomposition behavior
@Stable
private class Counter(var count: Int)

@Composable
private fun CountingLayout(modifier: Modifier, counter: Counter) {
    Layout(
        modifier = modifier,
        children = {}
    ) { _, constraints ->
        counter.count++
        layout(constraints.minWidth, constraints.minHeight) {}
    }
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably.  Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(modifier: Modifier = Modifier, children: @Composable () -> Unit) {
    Layout(modifier = modifier, children = children) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeables = measurables.map {
                it.measure(constraints)
            }
            val (width, height) = with(placeables.filterNotNull()) {
                Pair(
                    max(
                        maxByOrNull { it.width }?.width ?: 0,
                        constraints.minWidth
                    ),
                    max(
                        maxByOrNull { it.height }?.height ?: 0,
                        constraints.minHeight
                    )
                )
            }
            layout(width, height) {
                for (placeable in placeables) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}
