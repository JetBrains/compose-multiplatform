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

package androidx.compose.ui.text.android.style

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import androidx.annotation.IntDef
import androidx.compose.ui.text.android.InternalPlatformTextApi
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A span that is used to reserve empty spaces for inline element. It's used only to tell the
 * text processor the size and vertical alignment of the inline element.
 *
 * @param width The width needed by the inline element.
 * @param widthUnit The unit of the [width]; can be Sp, Em or inherit, if it's inherit, by
 * default it will be 1.em
 * @param height The height needed by the inline element.
 * @param heightUnit The unit of the [height]; can be Sp, Em or inherit, if it's inherit, by
 * default it will be computed from the context FontMetrics by
 * height = fontMetrics.descent - fontMetrics.ascent.
 * @param pxPerSp The number of pixels 1 Sp equals to.
 * @param verticalAlign How the inline element is aligned with the text.
 *
 * @suppress
 */
@InternalPlatformTextApi
class PlaceholderSpan(
    private val width: Float,
    @Unit
    private val widthUnit: Int,
    private val height: Float,
    @Unit
    private val heightUnit: Int,
    private val pxPerSp: Float,
    @VerticalAlign
    val verticalAlign: Int
) : ReplacementSpan() {
    companion object {
        const val ALIGN_ABOVE_BASELINE = 0
        const val ALIGN_TOP = 1
        const val ALIGN_BOTTOM = 2
        const val ALIGN_CENTER = 3
        const val ALIGN_TEXT_TOP = 4
        const val ALIGN_TEXT_BOTTOM = 5
        const val ALIGN_TEXT_CENTER = 6

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            ALIGN_ABOVE_BASELINE,
            ALIGN_TOP,
            ALIGN_BOTTOM,
            ALIGN_CENTER,
            ALIGN_TEXT_TOP,
            ALIGN_TEXT_BOTTOM,
            ALIGN_TEXT_CENTER
        )
        internal annotation class VerticalAlign

        const val UNIT_SP = 0
        const val UNIT_EM = 1
        const val UNIT_INHERIT = 2

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            UNIT_SP,
            UNIT_EM,
            UNIT_INHERIT
        )
        internal annotation class Unit
    }

    /* Used to compute bounding box when verticalAlign is ALIGN_TEXT_TOP / ALIGN_TEXT_BOTTOM. */
    lateinit var fontMetrics: Paint.FontMetricsInt
        private set

    /* The laid out width of the placeholder, in the unit of pixel. */
    var widthPx: Int = 0
        private set
        get() {
            check(isLaidOut) { "PlaceholderSpan is not laid out yet." }
            return field
        }

    /* The laid out height of the placeholder, in the unit of pixel. */
    var heightPx: Int = 0
        private set
        get() {
            check(isLaidOut) { "PlaceholderSpan is not laid out yet." }
            return field
        }

    /* Primitives can't be "lateinit", use this boolean to work around. */
    private var isLaidOut: Boolean = false

    @SuppressLint("DocumentExceptions")
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        isLaidOut = true
        val fontSize = paint.textSize
        fontMetrics = paint.fontMetricsInt
        require(fontMetrics.descent > fontMetrics.ascent) {
            "Invalid fontMetrics: line height can not be negative."
        }

        widthPx = when (widthUnit) {
            UNIT_SP -> width * pxPerSp
            UNIT_EM -> width * fontSize
            else -> throw IllegalArgumentException("Unsupported unit.")
        }.ceilToInt()

        heightPx = when (heightUnit) {
            UNIT_SP -> (height * pxPerSp).ceilToInt()
            UNIT_EM -> (height * fontSize).ceilToInt()
            else -> throw IllegalArgumentException("Unsupported unit.")
        }

        fm?.apply {
            ascent = fontMetrics.ascent
            descent = fontMetrics.descent
            leading = fontMetrics.leading

            when (verticalAlign) {
                // If align top and inline element is too tall, expand descent.
                ALIGN_TOP, ALIGN_TEXT_TOP -> if (ascent + heightPx > descent) {
                    descent = ascent + heightPx
                }
                // If align bottom and inline element is too tall, expand ascent.
                ALIGN_BOTTOM, ALIGN_TEXT_BOTTOM -> if (ascent > descent - heightPx) {
                    ascent = descent - heightPx
                }
                // If align center and inline element is too tall, evenly expand ascent and descent.
                ALIGN_CENTER, ALIGN_TEXT_CENTER -> if (descent - ascent < heightPx) {
                    ascent -= (heightPx - (descent - ascent)) / 2
                    descent = ascent + heightPx
                }
                // If align baseline and inline element is too tall, expand ascent
                ALIGN_ABOVE_BASELINE -> if (ascent > -heightPx) {
                    ascent = -heightPx
                }
                else -> throw IllegalArgumentException("Unknown verticalAlign.")
            }
            // make top/bottom at least same as ascent/descent.
            top = min(fontMetrics.top, ascent)
            bottom = max(fontMetrics.bottom, descent)
        }

        return widthPx
    }

    /* Empty implementation, not used. */
    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {}
}

internal fun Float.ceilToInt(): Int = ceil(this).toInt()
