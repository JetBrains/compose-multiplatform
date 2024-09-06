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
import com.example.common.generated.resources.Res

@Immutable
data class Snack(
    val id: Long,
    val name: String,
    val imageUrl: String,
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
        imageUrl = "files/cupcake.jpg",
        price = 299
    ),
    Snack(
        id = 2L,
        name = "Donut",
        tagline = "A tag line",
        imageUrl = "files/donut.jpg",
        price = 290
    ),
    Snack(
        id = 3L,
        name = "Eclair",
        tagline = "A tag line",
        imageUrl = "files/eclair.jpg",
        price = 289
    ),
    Snack(
        id = 4L,
        name = "Froyo",
        tagline = "A tag line",
        imageUrl = "files/froyo.jpg",
        price = 288
    ),
    Snack(
        id = 5L,
        name = "Gingerbread",
        tagline = "A tag line",
        imageUrl = "files/gingerbread.jpg",
        price = 499
    ),
    Snack(
        id = 6L,
        name = "Honeycomb",
        tagline = "A tag line",
        imageUrl = "files/honeycomb.jpg",
        price = 309
    ),
    Snack(
        id = 7L,
        name = "Ice Cream Sandwich",
        tagline = "A tag line",
        imageUrl = "files/ice_cream_sandwich.jpg",
        price = 1299
    ),
    Snack(
        id = 8L,
        name = "Jellybean",
        tagline = "A tag line",
        imageUrl = "files/jelly_bean.jpg",
        price = 109
    ),
    Snack(
        id = 9L,
        name = "KitKat",
        tagline = "A tag line",
        imageUrl = "files/kitkat.jpg",
        price = 549
    ),
    Snack(
        id = 10L,
        name = "Lollipop",
        tagline = "A tag line",
        imageUrl = "files/lollipop.jpg",
        price = 209
    ),
    Snack(
        id = 11L,
        name = "Marshmallow",
        tagline = "A tag line",
        imageUrl = "files/marshmallow.jpg",
        price = 219
    ),
    Snack(
        id = 12L,
        name = "Nougat",
        tagline = "A tag line",
        imageUrl = "files/nougat.jpg",
        price = 309
    ),
    Snack(
        id = 13L,
        name = "Oreo",
        tagline = "A tag line",
        imageUrl = "files/oreo.jpg",
        price = 339
    ),
    Snack(
        id = 14L,
        name = "Pie",
        tagline = "A tag line",
        imageUrl = "files/pie.jpg",
        price = 249
    ),
    Snack(
        id = 15L,
        name = "Chips",
        imageUrl = "files/chips.jpg",
        price = 277
    ),
    Snack(
        id = 16L,
        name = "Pretzels",
        imageUrl = "files/pretzels.jpg",
        price = 154
    ),
    Snack(
        id = 17L,
        name = "Smoothies",
        imageUrl = "files/smoothies.jpg",
        price = 257
    ),
    Snack(
        id = 18L,
        name = "Popcorn",
        imageUrl = "files/popcorn.jpg",
        price = 167
    ),
    Snack(
        id = 19L,
        name = "Almonds",
        imageUrl = "files/almonds.jpg",
        price = 123
    ),
    Snack(
        id = 20L,
        name = "Cheese",
        imageUrl = "files/cheese.jpg",
        price = 231
    ),
    Snack(
        id = 21L,
        name = "Apples",
        tagline = "A tag line",
        imageUrl = "files/apples.jpg",
        price = 221
    ),
    Snack(
        id = 22L,
        name = "Apple sauce",
        tagline = "A tag line",
        imageUrl = "files/apple_sauce.jpg",
        price = 222
    ),
    Snack(
        id = 23L,
        name = "Apple chips",
        tagline = "A tag line",
        imageUrl = "files/apple_chips.jpg",
        price = 231
    ),
    Snack(
        id = 24L,
        name = "Apple juice",
        tagline = "A tag line",
        imageUrl = "files/apple_juice.jpg",
        price = 241
    ),
    Snack(
        id = 25L,
        name = "Apple pie",
        tagline = "A tag line",
        imageUrl = "files/apple_pie.jpg",
        price = 225
    ),
    Snack(
        id = 26L,
        name = "Grapes",
        tagline = "A tag line",
        imageUrl = "files/grapes.jpg",
        price = 266
    ),
    Snack(
        id = 27L,
        name = "Kiwi",
        tagline = "A tag line",
        imageUrl = "files/kiwi.jpg",
        price = 127
    ),
    Snack(
        id = 28L,
        name = "Mango",
        tagline = "A tag line",
        imageUrl = "files/mango.jpg",
        price = 128
    )
)
