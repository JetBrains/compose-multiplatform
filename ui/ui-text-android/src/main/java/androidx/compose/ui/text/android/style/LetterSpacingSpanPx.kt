package androidx.compose.ui.text.android.style

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.annotation.Px
import androidx.compose.ui.text.android.InternalPlatformTextApi

/**
 * Span that sets the letter spacing as [letterSpacing], in the unit of pixel.
 *
 * @suppress
 */
@InternalPlatformTextApi
class LetterSpacingSpanPx(@Px val letterSpacing: Float) : MetricAffectingSpan() {
    private fun TextPaint.updatePaint() {
        // In framework, 1em letterSpacing equals to textSize * textScaleX pixels.
        val emWidth = textSize * textScaleX
        // Do nothing if emWidth is 0.0f.
        if (emWidth != 0.0f) {
            letterSpacing = this@LetterSpacingSpanPx.letterSpacing / emWidth
        }
    }

    override fun updateDrawState(textPaint: TextPaint) {
        textPaint.updatePaint()
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        textPaint.updatePaint()
    }
}