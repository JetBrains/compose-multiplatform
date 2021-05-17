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

package androidx.compose.ui.text.platform.extensions

import android.text.Spannable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.style.PlaceholderSpan
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.util.fastForEach

internal fun Spannable.setPlaceholders(
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density
) {
    placeholders.fastForEach {
        val (placeholder, start, end) = it
        setPlaceholder(placeholder, start, end, density)
    }
}

@OptIn(InternalPlatformTextApi::class)
private fun Spannable.setPlaceholder(
    placeholder: Placeholder,
    start: Int,
    end: Int,
    density: Density
) {
    setSpan(
        with(placeholder) {
            PlaceholderSpan(
                width = width.value,
                widthUnit = width.spanUnit,
                height = height.value,
                heightUnit = height.spanUnit,
                pxPerSp = density.fontScale * density.density,
                verticalAlign = placeholderVerticalAlign.spanVerticalAlign
            )
        },
        start,
        end
    )
}

/** Helper function that converts [TextUnit.type] to the unit in [PlaceholderSpan]. */
@OptIn(InternalPlatformTextApi::class)
@Suppress("DEPRECATION")
private val TextUnit.spanUnit: Int
    get() = when (type) {
        TextUnitType.Sp -> PlaceholderSpan.UNIT_SP
        TextUnitType.Em -> PlaceholderSpan.UNIT_EM
        else -> PlaceholderSpan.UNIT_UNSPECIFIED
    }

/**
 * Helper function that converts [PlaceholderVerticalAlign] to the verticalAlign in
 * [PlaceholderSpan].
 */
@OptIn(InternalPlatformTextApi::class)
private val PlaceholderVerticalAlign.spanVerticalAlign: Int
    get() = when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderSpan.ALIGN_ABOVE_BASELINE
        PlaceholderVerticalAlign.Top -> PlaceholderSpan.ALIGN_TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderSpan.ALIGN_BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderSpan.ALIGN_CENTER
        PlaceholderVerticalAlign.TextTop -> PlaceholderSpan.ALIGN_TEXT_TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderSpan.ALIGN_TEXT_BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderSpan.ALIGN_TEXT_CENTER
        else -> error("Invalid PlaceholderVerticalAlign")
    }
