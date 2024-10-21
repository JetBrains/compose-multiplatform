/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetsnack.model

import androidx.compose.runtime.Immutable
import com.example.common.generated.resources.*
import com.example.common.generated.resources.Res
import com.example.common.generated.resources.cupcake
import com.example.common.generated.resources.donut
import com.example.common.generated.resources.eclair
import org.jetbrains.compose.resources.DrawableResource

@Immutable
data class Snack(
    val id: Long,
    val name: String,
    val image: DrawableResource,
    val price: Long,
    val tagline: String = "",
    val tags: Set<String> = emptySet()
)

/**
 * Static data
 */

val snacks = listOf(
    Snack(
        id = 1L,
        name = "Cupcake",
        tagline = "A tag line",
        image = Res.drawable.cupcake,
        price = 299
    ),
    Snack(
        id = 2L,
        name = "Donut",
        tagline = "A tag line",
        image = Res.drawable.donut,
        price = 290
    ),
    Snack(
        id = 3L,
        name = "Eclair",
        tagline = "A tag line",
        image = Res.drawable.eclair,
        price = 289
    ),
    Snack(
        id = 4L,
        name = "Froyo",
        tagline = "A tag line",
        image = Res.drawable.froyo,
        price = 288
    ),
    Snack(
        id = 5L,
        name = "Gingerbread",
        tagline = "A tag line",
        image = Res.drawable.gingerbread,
        price = 499
    ),
    Snack(
        id = 6L,
        name = "Honeycomb",
        tagline = "A tag line",
        image = Res.drawable.honeycomb,
        price = 309
    ),
    Snack(
        id = 7L,
        name = "Ice Cream Sandwich",
        tagline = "A tag line",
        image = Res.drawable.ice_cream_sandwich,
        price = 1299
    ),
    Snack(
        id = 8L,
        name = "Jellybean",
        tagline = "A tag line",
        image = Res.drawable.jelly_bean,
        price = 109
    ),
    Snack(
        id = 9L,
        name = "KitKat",
        tagline = "A tag line",
        image = Res.drawable.kitkat,
        price = 549
    ),
    Snack(
        id = 10L,
        name = "Lollipop",
        tagline = "A tag line",
        image = Res.drawable.lollipop,
        price = 209
    ),
    Snack(
        id = 11L,
        name = "Marshmallow",
        tagline = "A tag line",
        image = Res.drawable.marshmallow,
        price = 219
    ),
    Snack(
        id = 12L,
        name = "Nougat",
        tagline = "A tag line",
        image = Res.drawable.nougat,
        price = 309
    ),
    Snack(
        id = 13L,
        name = "Oreo",
        tagline = "A tag line",
        image = Res.drawable.oreo,
        price = 339
    ),
    Snack(
        id = 14L,
        name = "Pie",
        tagline = "A tag line",
        image = Res.drawable.pie,
        price = 249
    ),
    Snack(
        id = 15L,
        name = "Chips",
        image = Res.drawable.chips,
        price = 277
    ),
    Snack(
        id = 16L,
        name = "Pretzels",
        image = Res.drawable.pretzels,
        price = 154
    ),
    Snack(
        id = 17L,
        name = "Smoothies",
        image = Res.drawable.smoothies,
        price = 257
    ),
    Snack(
        id = 18L,
        name = "Popcorn",
        image = Res.drawable.popcorn,
        price = 167
    ),
    Snack(
        id = 19L,
        name = "Almonds",
        image = Res.drawable.almonds,
        price = 123
    ),
    Snack(
        id = 20L,
        name = "Cheese",
        image = Res.drawable.cheese,
        price = 231
    ),
    Snack(
        id = 21L,
        name = "Apples",
        tagline = "A tag line",
        image = Res.drawable.apples,
        price = 221
    ),
    Snack(
        id = 22L,
        name = "Apple sauce",
        tagline = "A tag line",
        image = Res.drawable.apple_sauce,
        price = 222
    ),
    Snack(
        id = 23L,
        name = "Apple chips",
        tagline = "A tag line",
        image = Res.drawable.apple_chips,
        price = 231
    ),
    Snack(
        id = 24L,
        name = "Apple juice",
        tagline = "A tag line",
        image = Res.drawable.apple_juice,
        price = 241
    ),
    Snack(
        id = 25L,
        name = "Apple pie",
        tagline = "A tag line",
        image = Res.drawable.apple_pie,
        price = 225
    ),
    Snack(
        id = 26L,
        name = "Grapes",
        tagline = "A tag line",
        image = Res.drawable.grapes,
        price = 266
    ),
    Snack(
        id = 27L,
        name = "Kiwi",
        tagline = "A tag line",
        image = Res.drawable.kiwi,
        price = 127
    ),
    Snack(
        id = 28L,
        name = "Mango",
        tagline = "A tag line",
        image = Res.drawable.mango,
        price = 128
    )
)
