/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
import androidx.compose.foundation.newtext.text.TextUsingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.test.filters.LargeTest
import org.junit.Assume
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Toggle between missing Text and Text("aaa..") to simulate backend text loading.
 *
 * This intentionally hits as many text caches as possible, to isolate compose setText behavior.
 */
@OptIn(ExperimentalTextApi::class)
class ModifierIfNotEmptyCallText(
    private val text: String
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleText = mutableStateOf("")

    private val style = TextStyle.Default.copy(fontFamily = FontFamily.Monospace)

    @Composable
    override fun MeasuredContent() {
        if (toggleText.value.isNotEmpty()) {
            TextUsingModifier(
                text = toggleText.value,
                style = style
            )
        }
    }

    override fun toggleState() {
        if (toggleText.value == "") {
            toggleText.value = text
        } else {
            toggleText.value = ""
        }
    }
}

@LargeTest
@RunWith(Parameterized::class)
open class ModifierIfNotEmptyParent(
    private val size: Int
) : EmpiricalBench<ModifierIfNotEmptyCallText>() {

    override val caseFactory = {
        val text = generateCacheableStringOf(size)
        ModifierIfNotEmptyCallText(text)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = arrayOf()
    }
}

/**
 * Metrics determined from all apps
 */
@LargeTest
@RunWith(Parameterized::class)
class ModifierAllAppsIfNotEmptyCallText(size: Int) : ModifierIfNotEmptyParent(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = AllApps.TextLengths
    }
}

/**
 * Metrics for Chat-like apps.
 *
 * These apps typically have more longer strings, due to user generated content.
 */
@LargeTest
@RunWith(Parameterized::class)
class ModifierChatAppIfNotEmptyCallText(size: Int) : ModifierIfNotEmptyParent(size) {
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
