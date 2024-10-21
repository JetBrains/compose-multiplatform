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
import com.example.common.generated.resources.chips
import com.example.common.generated.resources.desserts
import com.example.common.generated.resources.fruit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource

/**
 * A fake repo for searching.
 */
object SearchRepo {
    fun getCategories(): List<SearchCategoryCollection> = searchCategoryCollections
    fun getSuggestions(): List<SearchSuggestionGroup> = searchSuggestions

    suspend fun search(query: String): List<Snack> = withContext(Dispatchers.Default) {
        delay(200L) // simulate an I/O delay
        snacks.filter { it.name.contains(query, ignoreCase = true) }
    }
}

@Immutable
data class SearchCategoryCollection(
    val id: Long,
    val name: String,
    val categories: List<SearchCategory>
)

@Immutable
data class SearchCategory(
    val name: String,
    val image: DrawableResource
)

@Immutable
data class SearchSuggestionGroup(
    val id: Long,
    val name: String,
    val suggestions: List<String>
)

/**
 * Static data
 */

private val searchCategoryCollections = listOf<SearchCategoryCollection>(
    SearchCategoryCollection(
        id = 0L,
        name = "Categories",
        categories = listOf(
            SearchCategory(
                name = "Chips & crackers",
                image = Res.drawable.chips
            ),
            SearchCategory(
                name = "Fruit snacks",
                image = Res.drawable.fruit
            ),
            SearchCategory(
                name = "Desserts",
                image = Res.drawable.desserts
            ),
            SearchCategory(
                name = "Nuts ",
                image = Res.drawable.nuts
            )
        )
    ),
    SearchCategoryCollection(
        id = 1L,
        name = "Lifestyles",
        categories = listOf(
            SearchCategory(
                name = "Organic",
                image = Res.drawable.organic
            ),
            SearchCategory(
                name = "Gluten Free",
                image = Res.drawable.gluten_free
            ),
            SearchCategory(
                name = "Paleo",
                image = Res.drawable.paleo
            ),
            SearchCategory(
                name = "Vegan",
                image = Res.drawable.vegan
            ),
            SearchCategory(
                name = "Vegitarian",
                image = Res.drawable.organic
            ),
            SearchCategory(
                name = "Whole30",
                image = Res.drawable.paleo
            )
        )
    )
)

private val searchSuggestions = listOf(
    SearchSuggestionGroup(
        id = 0L,
        name = "Recent searches",
        suggestions = listOf(
            "Cheese",
            "Apple Sauce"
        )
    ),
    SearchSuggestionGroup(
        id = 1L,
        name = "Popular searches",
        suggestions = listOf(
            "Organic",
            "Gluten Free",
            "Paleo",
            "Vegan",
            "Vegitarian",
            "Whole30"
        )
    )
)
