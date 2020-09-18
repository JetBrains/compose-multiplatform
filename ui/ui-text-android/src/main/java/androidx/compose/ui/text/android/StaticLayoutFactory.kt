/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.android

import android.os.Build
import android.text.Layout.Alignment
import android.text.StaticLayout
import android.text.StaticLayout.Builder
import android.text.TextDirectionHeuristic
import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.text.android.LayoutCompat.BreakStrategy
import androidx.compose.ui.text.android.LayoutCompat.HyphenationFrequency
import androidx.compose.ui.text.android.LayoutCompat.JustificationMode
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

@OptIn(InternalPlatformTextApi::class)
internal object StaticLayoutFactory {
    private const val TAG = "StaticLayoutFactory"
    private var isInitialized = false
    private var staticLayoutConstructor: Constructor<StaticLayout>? = null

    private fun obtainStaticLayoutConstructor() {
        if (isInitialized) return
        isInitialized = true
        try {
            staticLayoutConstructor =
                StaticLayout::class.java.getConstructor(
                    CharSequence::class.java,
                    Int::class.javaPrimitiveType, /* start */
                    Int::class.javaPrimitiveType, /* end */
                    TextPaint::class.java,
                    Int::class.javaPrimitiveType, /* width */
                    Alignment::class.java,
                    TextDirectionHeuristic::class.java,
                    Float::class.javaPrimitiveType, /* lineSpacingMultiplier */
                    Float::class.javaPrimitiveType, /* lineSpacingExtra */
                    Boolean::class.javaPrimitiveType, /* includePadding */
                    TruncateAt::class.java,
                    Int::class.javaPrimitiveType, /* ellipsizeWidth */
                    Int::class.javaPrimitiveType /* maxLines */
                )
        } catch (e: NoSuchMethodException) {
            staticLayoutConstructor = null
            Log.e(TAG, "unable to collect necessary constructor.")
        }
    }

    /**
     * Builder class for StaticLayout.
     *
     * @param text The text to be laid out, optionally with spans
     * @param paint The base paint used for layout
     * @param width The width in pixels
     */
    fun create(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
        paint: TextPaint,
        width: Int,
        textDir: TextDirectionHeuristic = LayoutCompat.DEFAULT_TEXT_DIRECTION_HEURISTIC,
        alignment: Alignment = LayoutCompat.DEFAULT_LAYOUT_ALIGNMENT,
        @IntRange(from = 0)
        maxLines: Int = LayoutCompat.DEFAULT_MAX_LINES,
        ellipsize: TruncateAt? = null,
        @IntRange(from = 0)
        ellipsizedWidth: Int = width,
        @FloatRange(from = 0.0)
        lineSpacingMultiplier: Float = LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
        lineSpacingExtra: Float = LayoutCompat.DEFAULT_LINESPACING_EXTRA,
        @JustificationMode
        justificationMode: Int = LayoutCompat.DEFAULT_JUSTIFICATION_MODE,
        includePadding: Boolean = LayoutCompat.DEFAULT_INCLUDE_PADDING,
        fallbackLineSpacing: Boolean = LayoutCompat.DEFAULT_FALLBACK_LINE_SPACING,
        @BreakStrategy
        breakStrategy: Int = LayoutCompat.DEFAULT_BREAK_STRATEGY,
        @HyphenationFrequency
        hyphenationFrequency: Int = LayoutCompat.DEFAULT_HYPHENATION_FREQUENCY,
        leftIndents: IntArray? = null,
        rightIndents: IntArray? = null
    ): StaticLayout {
        require(start in 0..end)
        require(end in 0..text.length)
        require(maxLines >= 0)
        require(width >= 0)
        require(ellipsizedWidth >= 0)
        require(lineSpacingMultiplier >= 0f)

        return if (Build.VERSION.SDK_INT >= 23) {
            Builder.obtain(text, start, end, paint, width)
                .apply {
                    setTextDirection(textDir)
                    setAlignment(alignment)
                    setMaxLines(maxLines)
                    setEllipsize(ellipsize)
                    setEllipsizedWidth(ellipsizedWidth)
                    setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                    if (Build.VERSION.SDK_INT >= 26) {
                        setJustificationMode(justificationMode)
                    }
                    setIncludePad(includePadding)
                    if (Build.VERSION.SDK_INT >= 28) {
                        setUseLineSpacingFromFallbacks(fallbackLineSpacing)
                    }
                    setBreakStrategy(breakStrategy)
                    setHyphenationFrequency(hyphenationFrequency)
                    setIndents(leftIndents, rightIndents)
                }.build()
        } else {
            // On API 21 to 23, try to call the StaticLayoutConstructor which supports the
            // textDir and maxLines.
            obtainStaticLayoutConstructor()
            staticLayoutConstructor?.let {
                try {
                    it.newInstance(
                        text,
                        start,
                        end,
                        paint,
                        width,
                        alignment,
                        textDir,
                        lineSpacingMultiplier,
                        lineSpacingExtra,
                        includePadding,
                        ellipsize,
                        ellipsizedWidth,
                        maxLines
                    )
                } catch (e: IllegalAccessException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                } catch (e: InstantiationException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                } catch (e: InvocationTargetException) {
                    staticLayoutConstructor = null
                    Log.e(TAG, "unable to call constructor")
                    null
                }
            }
                // On API 21 to 23 where it failed to find StaticLayout.Builder, create with
                // deprecated constructor, textDir and maxLines won't work in this case.
                ?: @Suppress("DEPRECATION") StaticLayout(
                    text,
                    start,
                    end,
                    paint,
                    width,
                    alignment,
                    lineSpacingMultiplier,
                    lineSpacingExtra,
                    includePadding,
                    ellipsize,
                    ellipsizedWidth
                )
        }
    }
}
