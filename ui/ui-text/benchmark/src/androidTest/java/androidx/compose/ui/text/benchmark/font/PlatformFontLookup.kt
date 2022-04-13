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

package androidx.compose.ui.text.benchmark.font

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.benchmark.cartesian
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.emptyCacheFontFamilyResolver
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PlatformFontLookup(val fontFamily: FontFamily, val fontWeight: FontWeight) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fontFamily={0} fontWeight={1}")
        fun initParameters() = cartesian(
            arrayOf(
                FontFamily.Default,
                FontFamily.SansSerif,
                FontFamily.Serif,
                FontFamily.Cursive,
                FontFamily.Monospace
            ),
            arrayOf(
                100,
                400,
                700
            ).map { FontWeight(it) }.toTypedArray()
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().context

    @OptIn(ExperimentalTextApi::class, InternalTextApi::class)
    @Test
    fun forceUncached() {
        benchmarkRule.measureRepeated {
            val fontFamilyResolver = runWithTimingDisabled {
                emptyCacheFontFamilyResolver(context)
            }
            fontFamilyResolver.resolve(fontFamily, fontWeight)
        }
    }

    @Test
    fun cached() {
        benchmarkRule.measureRepeated {
            val fontFamilyResolver = runWithTimingDisabled {
                createFontFamilyResolver(context)
            }
            fontFamilyResolver.resolve(fontFamily, fontWeight)
        }
    }
}