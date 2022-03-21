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
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.width
import kotlin.math.abs

/**
 * Asserts that the layout of this node has width equal to [expectedWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsEqualTo(expectedWidth: Dp): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.width.assertIsEqualTo(expectedWidth, "width")
    }
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsEqualTo(expectedHeight: Dp): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.height.assertIsEqualTo(expectedHeight, "height")
    }
}

/**
 * Asserts that the touch bounds of this node has width equal to [expectedWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertTouchWidthIsEqualTo(
    expectedWidth: Dp
): SemanticsNodeInteraction {
    return withTouchBoundsInRoot {
        it.width.assertIsEqualTo(expectedWidth, "width")
    }
}

/**
 * Asserts that the touch bounds of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertTouchHeightIsEqualTo(
    expectedHeight: Dp
): SemanticsNodeInteraction {
    return withTouchBoundsInRoot {
        it.height.assertIsEqualTo(expectedHeight, "height")
    }
}
/**
 * Asserts that the layout of this node has width that is greater than or equal to
 * [expectedMinWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsAtLeast(expectedMinWidth: Dp): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.width.assertIsAtLeast(expectedMinWidth, "width")
    }
}

/**
 * Asserts that the layout of this node has height that is greater than or equal to
 * [expectedMinHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.height.assertIsAtLeast(expectedMinHeight, "height")
    }
}

/**
 * Asserts that the layout of this node has position in the root composable that is equal to the
 * given position.
 *
 * @param expectedLeft The left (x) position to assert.
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertPositionInRootIsEqualTo(
    expectedLeft: Dp,
    expectedTop: Dp
): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.left.assertIsEqualTo(expectedLeft, "left")
        it.top.assertIsEqualTo(expectedTop, "top")
    }
}

/**
 * Asserts that the layout of this node has the top position in the root composable that is equal to
 * the given position.
 *
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertTopPositionInRootIsEqualTo(
    expectedTop: Dp
): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.top.assertIsEqualTo(expectedTop, "top")
    }
}

/**
 * Asserts that the layout of this node has the left position in the root composable that is
 * equal to the given position.
 *
 * @param expectedLeft The left (x) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertLeftPositionInRootIsEqualTo(
    expectedLeft: Dp
): SemanticsNodeInteraction {
    return withUnclippedBoundsInRoot {
        it.left.assertIsEqualTo(expectedLeft, "left")
    }
}

/**
 * Returns the bounds of the layout of this node. The bounds are relative to the root composable.
 */
fun SemanticsNodeInteraction.getUnclippedBoundsInRoot(): DpRect {
    lateinit var bounds: DpRect
    withUnclippedBoundsInRoot {
        bounds = it
    }
    return bounds
}

/**
 * Returns the bounds of the layout of this node as clipped to the root. The bounds are relative to
 * the root composable.
 */
fun SemanticsNodeInteraction.getBoundsInRoot(): DpRect {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    return with(node.layoutInfo.density) {
        node.boundsInRoot.let {
            DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
        }
    }
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
    val density = node.layoutInfo.density
    return operation.invoke(density, node)
}

private fun SemanticsNodeInteraction.withUnclippedBoundsInRoot(
    assertion: (DpRect) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val bounds = with(node.layoutInfo.density) {
        node.unclippedBoundsInRoot.let {
            DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
        }
    }
    assertion.invoke(bounds)
    return this
}

private fun SemanticsNodeInteraction.withTouchBoundsInRoot(
    assertion: (DpRect) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val bounds = with(node.layoutInfo.density) {
        node.touchBoundsInRoot.let {
            DpRect(it.left.toDp(), it.top.toDp(), it.right.toDp(), it.bottom.toDp())
        }
    }
    assertion.invoke(bounds)
    return this
}

private val SemanticsNode.unclippedBoundsInRoot: Rect
    get() {
        return if (layoutInfo.isPlaced) {
            Rect(positionInRoot, size.toSize())
        } else {
            Dp.Unspecified.value.let { Rect(it, it, it, it) }
        }
    }

/**
 * Returns if this value is equal to the [reference], within a given [tolerance]. If the
 * reference value is [Float.NaN], [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY], this
 * only returns true if this value is exactly the same (tolerance is disregarded).
 */
private fun Dp.isWithinTolerance(reference: Dp, tolerance: Dp): Boolean {
    return when {
        reference.isUnspecified -> this.isUnspecified
        reference.value.isInfinite() -> this.value == reference.value
        else -> abs(this.value - reference.value) <= tolerance.value
    }
}

/**
 * Asserts that this value is equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 *
 * @throws AssertionError if comparison fails.
 */
fun Dp.assertIsEqualTo(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!isWithinTolerance(expected, tolerance)) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected $expected (tolerance: $tolerance)"
        )
    }
}

/**
 * Asserts that this value is greater than or equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be greater than or equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 *
 * @throws AssertionError if comparison fails.
 */
private fun Dp.assertIsAtLeast(expected: Dp, subject: String, tolerance: Dp = Dp(.5f)) {
    if (!(isWithinTolerance(expected, tolerance) || (!isUnspecified && this > expected))) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected at least $expected (tolerance: $tolerance)"
        )
    }
}
