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

import androidx.compose.material3.tokens.Palette
import androidx.compose.ui.graphics.Color

/**
 * Tonal Palette structure in Material.
 *
 * A tonal palette is comprised of 5 tonal ranges. Each tonal range includes the 13 stops, or
 * tonal swatches.
 *
 * Tonal range names are:
 * - Neutral (N)
 * - Neutral variant (NV)
 * - Primary (P)
 * - Secondary (S)
 * - Tertiary (T)
 */
internal class TonalPalette(
    // The neutral tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [neutral100] to the darkest shade [neutral0].
    val neutral100: Color,
    val neutral99: Color,
    val neutral95: Color,
    val neutral90: Color,
    val neutral80: Color,
    val neutral70: Color,
    val neutral60: Color,
    val neutral50: Color,
    val neutral40: Color,
    val neutral30: Color,
    val neutral20: Color,
    val neutral10: Color,
    val neutral0: Color,

    // The neutral variant tonal range, sometimes called "neutral 2",  from the
    // generated dynamic color palette.
    // Ordered from the lightest shade [neutralVariant100] to the darkest shade [neutralVariant0].
    val neutralVariant100: Color,
    val neutralVariant99: Color,
    val neutralVariant95: Color,
    val neutralVariant90: Color,
    val neutralVariant80: Color,
    val neutralVariant70: Color,
    val neutralVariant60: Color,
    val neutralVariant50: Color,
    val neutralVariant40: Color,
    val neutralVariant30: Color,
    val neutralVariant20: Color,
    val neutralVariant10: Color,
    val neutralVariant0: Color,

    // The primary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [primary100] to the darkest shade [primary0].
    val primary100: Color,
    val primary99: Color,
    val primary95: Color,
    val primary90: Color,
    val primary80: Color,
    val primary70: Color,
    val primary60: Color,
    val primary50: Color,
    val primary40: Color,
    val primary30: Color,
    val primary20: Color,
    val primary10: Color,
    val primary0: Color,

    // The secondary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [secondary100] to the darkest shade [secondary0].
    val secondary100: Color,
    val secondary99: Color,
    val secondary95: Color,
    val secondary90: Color,
    val secondary80: Color,
    val secondary70: Color,
    val secondary60: Color,
    val secondary50: Color,
    val secondary40: Color,
    val secondary30: Color,
    val secondary20: Color,
    val secondary10: Color,
    val secondary0: Color,

    // The tertiary tonal range from the generated dynamic color palette.
    // Ordered from the lightest shade [tertiary100] to the darkest shade [tertiary0].
    val tertiary100: Color,
    val tertiary99: Color,
    val tertiary95: Color,
    val tertiary90: Color,
    val tertiary80: Color,
    val tertiary70: Color,
    val tertiary60: Color,
    val tertiary50: Color,
    val tertiary40: Color,
    val tertiary30: Color,
    val tertiary20: Color,
    val tertiary10: Color,
    val tertiary0: Color,
)

/**
 * Baseline colors in Material.
 */
internal val BaselineTonalPalette =
    TonalPalette(
        neutral100 = Palette.Neutral100,
        neutral99 = Palette.Neutral99,
        neutral95 = Palette.Neutral95,
        neutral90 = Palette.Neutral90,
        neutral80 = Palette.Neutral80,
        neutral70 = Palette.Neutral70,
        neutral60 = Palette.Neutral60,
        neutral50 = Palette.Neutral50,
        neutral40 = Palette.Neutral40,
        neutral30 = Palette.Neutral30,
        neutral20 = Palette.Neutral20,
        neutral10 = Palette.Neutral10,
        neutral0 = Palette.Neutral0,
        neutralVariant100 = Palette.NeutralVariant100,
        neutralVariant99 = Palette.NeutralVariant99,
        neutralVariant95 = Palette.NeutralVariant95,
        neutralVariant90 = Palette.NeutralVariant90,
        neutralVariant80 = Palette.NeutralVariant80,
        neutralVariant70 = Palette.NeutralVariant70,
        neutralVariant60 = Palette.NeutralVariant60,
        neutralVariant50 = Palette.NeutralVariant50,
        neutralVariant40 = Palette.NeutralVariant40,
        neutralVariant30 = Palette.NeutralVariant30,
        neutralVariant20 = Palette.NeutralVariant20,
        neutralVariant10 = Palette.NeutralVariant10,
        neutralVariant0 = Palette.NeutralVariant0,
        primary100 = Palette.Primary100,
        primary99 = Palette.Primary99,
        primary95 = Palette.Primary95,
        primary90 = Palette.Primary90,
        primary80 = Palette.Primary80,
        primary70 = Palette.Primary70,
        primary60 = Palette.Primary60,
        primary50 = Palette.Primary50,
        primary40 = Palette.Primary40,
        primary30 = Palette.Primary30,
        primary20 = Palette.Primary20,
        primary10 = Palette.Primary10,
        primary0 = Palette.Primary0,
        secondary100 = Palette.Secondary100,
        secondary99 = Palette.Secondary99,
        secondary95 = Palette.Secondary95,
        secondary90 = Palette.Secondary90,
        secondary80 = Palette.Secondary80,
        secondary70 = Palette.Secondary70,
        secondary60 = Palette.Secondary60,
        secondary50 = Palette.Secondary50,
        secondary40 = Palette.Secondary40,
        secondary30 = Palette.Secondary30,
        secondary20 = Palette.Secondary20,
        secondary10 = Palette.Secondary10,
        secondary0 = Palette.Secondary0,
        tertiary100 = Palette.Tertiary100,
        tertiary99 = Palette.Tertiary99,
        tertiary95 = Palette.Tertiary95,
        tertiary90 = Palette.Tertiary90,
        tertiary80 = Palette.Tertiary80,
        tertiary70 = Palette.Tertiary70,
        tertiary60 = Palette.Tertiary60,
        tertiary50 = Palette.Tertiary50,
        tertiary40 = Palette.Tertiary40,
        tertiary30 = Palette.Tertiary30,
        tertiary20 = Palette.Tertiary20,
        tertiary10 = Palette.Tertiary10,
        tertiary0 = Palette.Tertiary0,
    )
