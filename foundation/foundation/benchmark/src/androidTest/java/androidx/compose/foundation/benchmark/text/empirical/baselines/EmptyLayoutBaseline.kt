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

package androidx.compose.foundation.benchmark.text.empirical.baselines

import androidx.compose.foundation.benchmark.text.DoFullBenchmark
import androidx.compose.foundation.benchmark.text.empirical.AllApps
import androidx.compose.foundation.benchmark.text.empirical.ChatApps
import androidx.compose.foundation.benchmark.text.empirical.generateCacheableStringOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.test.filters.LargeTest
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This benchmark emits an empty Layout and does not do _any_ Text work.
 *
 * Any impl. of text must emit at least one Layout node sized on soft wrapping, so this is a floor
 * of work that Text could theoretically do if everything were fetched from a hypothetical free
 * cache that could compute results for future requests magically..
 *
 * Text will never be able to be _this_ fast (as we don't _yet_ have time-travel chips), but it is
 * useful to use this number as a floor when evaluating potential optimizations.
 */
class EmptyLayoutBaseline(
    private val text: String
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleText = mutableStateOf("")

    private val measurePolicy = MeasurePolicy { _, _ ->
        val width = 300
        val length = toggleText.value.length
        val height = /* line height */ 20 * (length / (/* width */ 300 / /* char width */ 12))
        layout(width, height.coerceAtLeast(20)) {}
    }

    @Composable
    override fun MeasuredContent() {
        // emit a layout based on text length, which is the minimal work that Text must perform
        Layout(modifier = modifier, measurePolicy = measurePolicy)
    }

    override fun toggleState() {
        if (toggleText.value == "") {
            toggleText.value = text
        } else {
            toggleText.value = ""
        }
    }
}

private val modifier = Modifier.fillMaxSize()

@LargeTest
@RunWith(Parameterized::class)
open class EmptyLayoutBaselineParent(private val size: Int) {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactory = {
        val text = generateCacheableStringOf(size)
        EmptyLayoutBaseline(text)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = arrayOf()
    }

    @Test
    fun recomposeOnly() {
        benchmarkRule.toggleStateBenchmarkRecompose(caseFactory, requireRecomposition = false)
    }

    @Test
    fun recomposeMeasureLayout() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory,
            requireRecomposition = false
        )
    }
}

@LargeTest
@RunWith(Parameterized::class)
class AllAppsEmptyLayoutBaselineBaseline(size: Int) : EmptyLayoutBaselineParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = AllApps.TextLengths
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ChatAppEmptyLayoutBaselineBaseline(size: Int) : EmptyLayoutBaselineParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = ChatApps.TextLengths
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}
