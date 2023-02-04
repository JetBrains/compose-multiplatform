/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.test.filters.LargeTest
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.reflect.KProperty0
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test to ensure equality (both structurally, and visually) between programmatically generated
 * extended Material [androidx.compose.material.icons.Icons] and their XML source.
 */
@Suppress("unused")
@LargeTest
@RunWith(Parameterized::class)
class ExtendedIconComparisonTest(
    private val iconSublist: List<Pair<KProperty0<ImageVector>, String>>,
    private val debugParameterName: String
) : BaseIconComparisonTest() {

    companion object {
        /**
         * Arbitrarily split [AllExtendedIcons] into equal parts. This is needed as one test with
         * the whole of [AllExtendedIcons] will exceed the timeout allowed for a test in CI, so we
         * split it up to stay under the limit.
         *
         * Additionally, we run large batches of comparisons per method, instead of one icon per
         * method, so that we can re-use the same Activity instance between test runs. Most of the
         * cost of a simple test like this is in Activity instantiation so re-using the same
         * activity reduces time to run this test ~tenfold.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initIconSublist(): Array<Array<Any>> {
            val numberOfChunks = 6
            val listSize = ceil(AllExtendedIcons.size / numberOfChunks.toFloat()).roundToInt()
            val subLists = AllExtendedIcons.chunked(listSize)
            return subLists.mapIndexed { index, list ->
                arrayOf(list, "${index + 1}of$numberOfChunks")
            }.toTypedArray()
        }
    }

    @Ignore("For performance reasons, and to be able to disable the extended icons tests on " +
        "AOSP. Make sure to execute locally after updating the extended Material icons.")
    @Test
    fun compareImageVectors() {
        compareImageVectors(iconSublist)
    }
}
