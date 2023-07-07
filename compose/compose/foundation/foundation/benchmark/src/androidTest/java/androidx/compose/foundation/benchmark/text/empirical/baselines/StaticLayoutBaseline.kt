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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.benchmark.text.DoFullBenchmark
import androidx.compose.foundation.benchmark.text.empirical.AllApps
import androidx.compose.foundation.benchmark.text.empirical.ChatApps
import androidx.compose.foundation.benchmark.text.empirical.generateCacheableStringOf
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This benchmark tests the *absolute* lower bound of drawing perfectly optimal text on Android.
 *
 * It intentionally hits the word-layout cache and emits monospace.
 *
 * No real-world text usage will lay out this fast, but it provides a useful check for the lowest
 * cost possible in text layout when evaluating potential optimizations.
 */
@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = 23)
open class StaticLayoutBaseline(private val size: Int) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = emptyArray()
    }

    @Test
    fun constructLayoutDraw() {
        val text = generateCacheableStringOf(size)
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.MONOSPACE
        val canvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        var isEmpty = false
        benchmarkRule.measureRepeated {
            val measureText = runWithTimingDisabled {
                isEmpty = !isEmpty
                if (isEmpty) "" else text
            }
            val layout = makeStaticLayout(measureText, textPaint)
            layout.draw(canvas)
        }
    }
}

@RunWith(Parameterized::class)
@LargeTest
class AllAppsStaticLayoutBaseline(size: Int) : StaticLayoutBaseline(size) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun initParameters(): Array<Any> = AllApps.TextLengths
    }
}

@RunWith(Parameterized::class)
@LargeTest
class ChatAppsStaticLayoutBaseline(size: Int) : StaticLayoutBaseline(size) {
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

// do compat code to make a realistic lowest-possible benchmark for API21+
// since StaticLayout.Builder is 23+
fun makeStaticLayout(text: String, textPaint: TextPaint): StaticLayout {
    return if (Build.VERSION.SDK_INT >= 23) {
        StaticLayoutBuilderCompat_Api23.build(text, textPaint)
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(/* source = */ text, /* paint = */
            textPaint, /* width = */
            Int.MAX_VALUE, /* align = */
            Layout.Alignment.ALIGN_NORMAL, /* spacingmult = */
            1.0f, /* spacingadd = */
            0.0f, /* includepad = */
            true)
    }
}

@RequiresApi(23)
object StaticLayoutBuilderCompat_Api23 {
    fun build(text: String, textPaint: TextPaint): StaticLayout {
        return StaticLayout.Builder.obtain(/* source = */ text, /* start = */
            0, /* end = */
            text.length, /* paint = */
            textPaint, /* width = */
            Int.MAX_VALUE).build()
    }
}