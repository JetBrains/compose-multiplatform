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

package androidx.compose.foundation.benchmark.text

import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasureLayout
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The benchmark for a Text composable with ellipsis and restricted height.
 */
@LargeTest
@RunWith(Parameterized::class)
class TextEllipsisBenchmark(
    private val textLength: Int
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0}")
        fun initParameters(): Array<Any> = arrayOf(32, 128, 512).filterForCi { max() }
    }

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val width = textBenchmarkRule.widthDp.dp
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    private val caseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            /**
             * Text render has a word cache in the underlying system. To get a proper metric of its
             * performance, the cache needs to be disabled, which unfortunately is not doable via
             * public API. Here is a workaround which generates a new string when a new test case
             * is created.
             */
            val texts = List(textBenchmarkRule.repeatTimes) {
                textGenerator.nextParagraph(textLength)
            }
            TextWithEllipsisTestCase(
                texts = texts,
                width = width,
                fontSize = fontSize
            )
        }
    }

    /**
     * Measure the time taken by the first time measure the a Text composable with the given input.
     * This is mainly the time used to measure all the Measurables in the a Text composable.
     */
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    /**
     * Measure the time taken by the first time layout the a Text composable with the given input.
     * This is mainly the time used to place Placeables in a Text composable.
     */
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    /**
     * Measure the time taken to layout the a Text composable when alignment gets toggled.
     */
    @Test
    fun toggleAlignment_measureLayout() {
        benchmarkRule.toggleStateBenchmarkMeasureLayout(caseFactory, assertOneRecomposition = false)
    }
}