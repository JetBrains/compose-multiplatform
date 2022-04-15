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

package androidx.compose.ui.text

/**
 * Provides configuration options for behavior compatibility for TextStyle.
 */
actual class PlatformTextStyle {
    actual val spanStyle: PlatformSpanStyle?
    actual val paragraphStyle: PlatformParagraphStyle?

    constructor(
        spanStyle: PlatformSpanStyle?,
        paragraphStyle: PlatformParagraphStyle?
    ) {
        this.spanStyle = spanStyle
        this.paragraphStyle = paragraphStyle
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformTextStyle) return false
        if (paragraphStyle != other.paragraphStyle) return false
        if (spanStyle != other.spanStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

internal actual fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle {
    return PlatformTextStyle(spanStyle, paragraphStyle)
}

/**
 * Provides configuration options for behavior compatibility for SpanStyle.
 */
actual class PlatformParagraphStyle {
    actual companion object {
        actual val Default: PlatformParagraphStyle = PlatformParagraphStyle()
    }

    actual fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformParagraphStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * Provides configuration options for behavior compatibility for SpanStyle.
 */
actual class PlatformSpanStyle {
    actual companion object {
        actual val Default: PlatformSpanStyle = PlatformSpanStyle()
    }

    actual fun merge(other: PlatformSpanStyle?): PlatformSpanStyle {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformSpanStyle) return false
        return true
    }

    @Suppress("RedundantOverride")
    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * Interpolate between two PlatformParagraphStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
actual fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle {
    return start
}

/**
 * Interpolate between two PlatformSpanStyle's.
 *
 * This will not work well if the styles don't set the same fields.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid.
 */
@ExperimentalTextApi
actual fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle {
    return start
}