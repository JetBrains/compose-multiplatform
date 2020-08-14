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

package androidx.compose.foundation.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DpConstraintsTest {

    @Test
    fun constructor() {
        val defaultDpConstraints = DpConstraints()
        defaultDpConstraints.assertEquals(0.dp, Dp.Infinity, 0.dp, Dp.Infinity)

        val constraints = DpConstraints(0.dp, 1.dp, 2.dp, 3.dp)
        constraints.assertEquals(0.dp, 1.dp, 2.dp, 3.dp)

        val tightDpConstraintsForWidth = DpConstraints.fixedWidth(5.dp)
        tightDpConstraintsForWidth.assertEquals(5.dp, 5.dp, 0.dp, Dp.Infinity)

        val tightDpConstraintsForHeight = DpConstraints.fixedHeight(5.dp)
        tightDpConstraintsForHeight.assertEquals(0.dp, Dp.Infinity, 5.dp, 5.dp)

        val tightDpConstraints = DpConstraints.fixed(5.dp, 7.dp)
        tightDpConstraints.assertEquals(5.dp, 5.dp, 7.dp, 7.dp)
    }

    @Test
    fun hasBoundedDimensions() {
        val unbounded = DpConstraints(3.dp, Dp.Infinity, 3.dp, Dp.Infinity)
        assertFalse(unbounded.hasBoundedWidth)
        assertFalse(unbounded.hasBoundedHeight)

        val bounded = DpConstraints(3.dp, 5.dp, 3.dp, 5.dp)
        assertTrue(bounded.hasBoundedWidth)
        assertTrue(bounded.hasBoundedHeight)
    }

    @Test
    fun hasFixedDimensions() {
        val untight = DpConstraints(3.dp, 4.dp, 8.dp, 9.dp)
        assertFalse(untight.hasFixedWidth)
        assertFalse(untight.hasFixedHeight)

        val tight = DpConstraints(3.dp, 3.dp, 5.dp, 5.dp)
        assertTrue(tight.hasFixedWidth)
        assertTrue(tight.hasFixedHeight)
    }

    @Test
    fun isZero() {
        val nonZero = DpConstraints(1.dp, 2.dp, 1.dp, 2.dp)
        assertFalse(nonZero.isZero)

        val zero = DpConstraints(0.dp, 0.dp, 0.dp, 0.dp)
        assertTrue(zero.isZero)
    }

    @Test
    fun enforce() {
        val constraints = DpConstraints(5.dp, 10.dp, 5.dp, 10.dp)
        constraints.enforce(DpConstraints(4.dp, 11.dp, 4.dp, 11.dp)).assertEquals(
            5.dp, 10.dp, 5.dp, 10.dp
        )
        constraints.enforce(DpConstraints(7.dp, 9.dp, 7.dp, 9.dp)).assertEquals(
            7.dp, 9.dp, 7.dp, 9.dp
        )
        constraints.enforce(DpConstraints(2.dp, 3.dp, 2.dp, 3.dp)).assertEquals(
            3.dp, 3.dp, 3.dp, 3.dp
        )
        constraints.enforce(DpConstraints(10.dp, 11.dp, 10.dp, 11.dp)).assertEquals(
            10.dp, 10.dp, 10.dp, 10.dp
        )
    }

    @Test
    fun offset() {
        val constraints = DpConstraints(2.dp, 2.dp, 5.dp, 5.dp)
        constraints.offset(horizontal = 2.dp, vertical = 3.dp).assertEquals(
            4.dp, 4.dp, 8.dp, 8.dp
        )
        constraints.offset(horizontal = -7.dp, vertical = -7.dp).assertEquals(
            0.dp, 0.dp, 0.dp, 0.dp
        )
    }

    @Test
    fun validity() {
        assertInvalid(minWidth = Dp.Infinity)
        assertInvalid(minHeight = Dp.Infinity)
        assertInvalid(minWidth = Float.NaN.dp)
        assertInvalid(maxWidth = Float.NaN.dp)
        assertInvalid(minHeight = Float.NaN.dp)
        assertInvalid(maxHeight = Float.NaN.dp)
        assertInvalid(minWidth = 3.dp, maxWidth = 2.dp)
        assertInvalid(minHeight = 3.dp, maxHeight = 2.dp)
        assertInvalid(minWidth = -1.dp)
        assertInvalid(maxWidth = -1.dp)
        assertInvalid(minHeight = -1.dp)
        assertInvalid(maxHeight = -1.dp)
    }

    private fun DpConstraints.assertEquals(
        minWidth: Dp,
        maxWidth: Dp,
        minHeight: Dp,
        maxHeight: Dp
    ) {
        assertTrue(this.minWidth == minWidth && this.maxWidth == maxWidth &&
                this.minHeight == minHeight && this.maxHeight == maxHeight)
    }

    private fun assertInvalid(
        minWidth: Dp = 0.dp,
        maxWidth: Dp = 0.dp,
        minHeight: Dp = 0.dp,
        maxHeight: Dp = 0.dp
    ) {
        val constraints: DpConstraints
        try {
            constraints = DpConstraints(minWidth, maxWidth, minHeight, maxHeight)
        } catch (_: IllegalArgumentException) {
            return
        }
        fail("Invalid constraints $constraints are considered valid")
    }
}
