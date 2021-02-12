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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect

/**
 * Asserts that the layout of this node has width equal to [expectedWidth].
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertWidthIsEqualTo(expectedWidth: Dp): SemanticsNodeInteraction =
    assert(hasWidth(expectedWidth))

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertHeightIsEqualTo(expectedHeight: Dp): SemanticsNodeInteraction =
    assert(hasHeight(expectedHeight))

/**
 * Asserts that the layout of this node has width that is greater ot equal to [expectedMinWidth].
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertWidthIsAtLeast(expectedMinWidth: Dp): SemanticsNodeInteraction =
    assert(hasWidthAtLeast(expectedMinWidth))

/**
 * Asserts that the layout of this node has height that is greater ot equal to [expectedMinHeight].
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
): SemanticsNodeInteraction =
    assert(hasHeightAtLeast(expectedMinHeight))

/**
 * Asserts that the layout of this node has position in the root composable that is equal to the
 * given position.
 *
 * @param expectedLeft The left (x) position to assert.
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertPositionInRootIsEqualTo(
    expectedLeft: Dp,
    expectedTop: Dp
): SemanticsNodeInteraction =
    assert(hasLeftPosition(expectedLeft).and(hasTopPosition(expectedTop)))

/**
 * Asserts that the layout of this node has the top position in the root composable that is equal to
 * the given position.
 *
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertTopPositionInRootIsEqualTo(
    expectedTop: Dp
): SemanticsNodeInteraction =
    assert(hasTopPosition(expectedTop))

/**
 * Asserts that the layout of this node has the left position in the root composable that is
 * equal to the given position.
 *
 * @param expectedLeft The left (x) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteraction.assertLeftPositionInRootIsEqualTo(
    expectedLeft: Dp
): SemanticsNodeInteraction =
    assert(hasLeftPosition(expectedLeft))

/**
 * Returns the bounds of the layout of this node. The bounds are relative to the root composable.
 */
fun SemanticsNodeInteraction.getUnclippedBoundsInRoot(): DpRect {
    lateinit var bounds: DpRect
    withUnclippedBoundsInRoot {
        bounds = DpRect(
            left = it.left.toDp(),
            top = it.top.toDp(),
            right = it.right.toDp(),
            bottom = it.bottom.toDp()
        )
    }
    return bounds
}

/**
 * Returns the position of an [alignment line][AlignmentLine], or [Dp.Unspecified] if the line is
 * not provided.
 */
fun SemanticsNodeInteraction.getAlignmentLinePosition(alignmentLine: AlignmentLine): Dp {
    return withDensity {
        val pos = it.getAlignmentLinePosition(alignmentLine)
        if (pos == AlignmentLine.Unspecified) {
            Dp.Unspecified
        } else {
            pos.toDp()
        }
    }
}

private fun <R> SemanticsNodeInteraction.withDensity(
    operation: Density.(SemanticsNode) -> R
): R {
    val node = fetchSemanticsNode("Failed to retrieve density for the node.")
    val density = node.root!!.density
    return operation.invoke(density, node)
}

private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
    assertion: Density.(Rect) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val density = node.root!!.density

    assertion.invoke(density, node.unclippedBoundsInRoot)
    return this
}
