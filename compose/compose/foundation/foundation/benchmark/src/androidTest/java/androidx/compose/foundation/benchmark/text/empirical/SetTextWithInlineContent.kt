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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import org.junit.Assume
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Inline content is equivalent to replacementSpan, which typically happens in 1-2 per string
 *
 * They are relatively rare overall.
 *
 * This benchmark only adds one replacement span, which is a typical case.
 */
class SetTextWithInlineContent(
    private val text: AnnotatedString
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleText = mutableStateOf(AnnotatedString(""))

    @Composable
    override fun MeasuredContent() {
        Text(toggleText.value,
            fontFamily = FontFamily.Monospace,
            inlineContent = mapOf(
                BenchmarkInlineContentId to InlineTextContent(
                    Placeholder(12.sp, 12.sp, PlaceholderVerticalAlign.Center)
                ) {
                    Box(Modifier.size(12.dp, 12.dp))
                }
            )
        )
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
open class SetTextWithInlineContentParent(
    private val size: Int
) : EmpiricalBench<SetTextWithInlineContent>() {

    override val caseFactory = {
        val text = generateCacheableStringOf(size)
        SetTextWithInlineContent(text.annotateWithInlineContent())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): List<Array<Any>> = listOf()
    }
}

@LargeTest
@RunWith(Parameterized::class)
class AllAppsWithInlineContent(size: Int) : SetTextWithInlineContentParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters() = AllApps.TextLengths
    }
}

@LargeTest
@RunWith(Parameterized::class)
class SocialAppWithInlineContent(size: Int) : SetTextWithInlineContentParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters() = SocialApps.TextLengths
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ChatAppWithInlineContent(size: Int) : SetTextWithInlineContentParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters() = ChatApps.TextLengths
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}

@LargeTest
@RunWith(Parameterized::class)
class ShoppingAppWithInlineContent(size: Int) : SetTextWithInlineContentParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters() = ShoppingApps.TextLengths
    }

    init {
        // we only need this for full reporting
        Assume.assumeTrue(DoFullBenchmark)
    }
}