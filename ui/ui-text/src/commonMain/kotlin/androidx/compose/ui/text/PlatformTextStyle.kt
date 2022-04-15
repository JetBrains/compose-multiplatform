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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text

/**
 * Provides platform specific [TextStyle] configuration options for styling and compatibility.
 */
@ExperimentalTextApi
expect class PlatformTextStyle {
    /**
     * Platform specific text span styling and compatibility configuration.
     */
    val spanStyle: PlatformSpanStyle?

    /**
     * Platform specific paragraph styling and compatibility configuration.
     */
    val paragraphStyle: PlatformParagraphStyle?
}

internal expect fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle

/**
 * Provides platform specific [ParagraphStyle] configuration options for styling and compatibility.
 */
@ExperimentalTextApi
expect class PlatformParagraphStyle {
    companion object {
        val Default: PlatformParagraphStyle
    }

    fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle
}

/**
 * Provides platform specific [SpanStyle] configuration options for styling and compatibility.
 */
@ExperimentalTextApi
expect class PlatformSpanStyle {
    companion object {
        val Default: PlatformSpanStyle
    }

    fun merge(other: PlatformSpanStyle?): PlatformSpanStyle
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
@ExperimentalTextApi
expect fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle

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
expect fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle