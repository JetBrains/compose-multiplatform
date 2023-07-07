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

package androidx.compose.foundation.benchmark.text.empirical

import androidx.compose.foundation.benchmark.text.DoFullBenchmark
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Show the behavior of several spans stacked on top of each other, for the full length of the text.
 *
 * 38% of all text displayed contains at least 1 span. The most common # of spans is 16 (due to
 * usage of spans as text formatting).
 *
 * Spans are intentionally limited to
 *
 * 1) Not MetricsAffectingSpans (usage is very low)
 * 2) Not inlineContent (usage is very low).
 *
 * Note: If introducing more optimizations that depend on the "full length" assumption, confirm the
 * frequency of spans that use the full length. This is not verified in the data set that produced
 * this benchmark. This assumption does not, currently, impact the performance of Compose.
 */
class SetTextWithSpans(
    private val text: AnnotatedString
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleText = mutableStateOf(AnnotatedString(""))

    @Composable
    override fun MeasuredContent() {
        Text(toggleText.value, fontFamily = FontFamily.Monospace)
    }

    override fun toggleState() {
        if (toggleText.value.text.isEmpty()) {
            toggleText.value = text
        } else {
            toggleText.value = AnnotatedString("")
        }
    }
}

@LargeTest
@RunWith(Parameterized::class)
open class SetTextWithSpansParent(
    private val size: Int,
    private val spanCount: Int
) : EmpiricalBench<SetTextWithSpans>() {
    override val caseFactory = {
        val text = generateCacheableStringOf(size)
        SetTextWithSpans(text.annotateWithSpans(spanCount))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters(): List<Array<Any>> = listOf()
    }
}

@LargeTest
@RunWith(Parameterized::class)
class AllAppsWithSpans(size: Int, spanCount: Int) : SetTextWithSpansParent(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = AllApps.TextLengthsWithSpans
    }
}

@LargeTest
@RunWith(Parameterized::class)
class SocialAppWithSpans(size: Int, spanCount: Int) : SetTextWithSpansParent(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = SocialApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ChatAppWithSpans(size: Int, spanCount: Int) : SetTextWithSpansParent(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = ChatApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ShoppingAppWithSpans(size: Int, spanCount: Int) : SetTextWithSpansParent(size, spanCount) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}, spanCount={1}")
        fun initParameters() = ShoppingApps.TextLengthsWithSpans
    }

    init {
        // we only need this for full reporting
        assumeTrue(DoFullBenchmark)
    }
}