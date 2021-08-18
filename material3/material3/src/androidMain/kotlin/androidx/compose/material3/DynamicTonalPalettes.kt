/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import android.content.Context
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color

/** Dynamic colors in Material3. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun dynamicTonalPalettes(context: Context): TonalPalettes =
    TonalPalettes(
        // The neutral tonal palette from the generated dynamic color palettes.
        neutral0 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_0),
        neutral10 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_10),
        neutral50 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_50),
        neutral100 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_100),
        neutral200 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_200),
        neutral300 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_300),
        neutral400 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_400),
        neutral500 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_500),
        neutral600 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_600),
        neutral700 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_700),
        neutral800 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_800),
        neutral900 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_900),
        neutral1000 = ColorResourceHelper.getColor(context, android.R.color.system_neutral1_1000),

        // The neutral variant tonal palette, sometimes called "neutral 2",  from the
        // generated dynamic color palettes.
        neutralVariant0 = ColorResourceHelper.getColor(context, android.R.color.system_neutral2_0),
        neutralVariant10 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_10),
        neutralVariant50 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_50),
        neutralVariant100 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_100),
        neutralVariant200 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_200),
        neutralVariant300 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_300),
        neutralVariant400 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_400),
        neutralVariant500 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_500),
        neutralVariant600 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_600),
        neutralVariant700 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_700),
        neutralVariant800 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_800),
        neutralVariant900 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_900),
        neutralVariant1000 =
            ColorResourceHelper.getColor(context, android.R.color.system_neutral2_1000),

        // The primary tonal palette from the generated dynamic color palettes.
        primary0 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_0),
        primary10 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_10),
        primary50 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_50),
        primary100 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_100),
        primary200 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_200),
        primary300 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_300),
        primary400 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_400),
        primary500 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_500),
        primary600 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_600),
        primary700 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_700),
        primary800 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_800),
        primary900 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_900),
        primary1000 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent1_1000),

        // The secondary tonal palette from the generated dynamic color palettes.
        secondary0 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_0),
        secondary10 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_10),
        secondary50 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_50),
        secondary100 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_100),
        secondary200 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_200),
        secondary300 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_300),
        secondary400 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_400),
        secondary500 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_500),
        secondary600 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_600),
        secondary700 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_700),
        secondary800 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_800),
        secondary900 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_900),
        secondary1000 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent2_1000),

        // The tertiary tonal palette from the generated dynamic color palettes.
        tertiary0 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_0),
        tertiary10 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_10),
        tertiary50 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_50),
        tertiary100 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_100),
        tertiary200 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_200),
        tertiary300 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_300),
        tertiary400 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_400),
        tertiary500 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_500),
        tertiary600 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_600),
        tertiary700 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_700),
        tertiary800 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_800),
        tertiary900 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_900),

        tertiary1000 =
            ColorResourceHelper.getColor(context, android.R.color.system_accent3_1000),
    )

@RequiresApi(23)
private object ColorResourceHelper {
    @DoNotInline
    fun getColor(context: Context, @ColorRes id: Int): Color {
        return Color(context.resources.getColor(id, context.theme))
    }
}
