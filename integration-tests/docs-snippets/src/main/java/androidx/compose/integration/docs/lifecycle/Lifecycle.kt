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

// Ignore lint warnings in documentation snippets
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "SimplifyBooleanWithConstants"
)

package androidx.compose.integration.docs.lifecycle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/lifecycle
 *
 * No action required if it's modified.
 */

private object LifecycleSnippet1 {
    @Composable
    fun MyComposable() {
        Column {
            Text("Hello")
            Text("World")
        }
    }
}
private object LifecycleSnippet2 {
    @Composable
    fun LoginScreen(showError: Boolean) {
        if (showError) {
            LoginError()
        }
        LoginInput() // This call site affects where LoginInput is placed in Composition
    }

    @Composable
    fun LoginInput() { /* ... */ }
}

private object LifecycleSnippet3 {
    @Composable
    fun MoviesScreen(movies: List<Movie>) {
        Column {
            for (movie in movies) {
                // MovieOverview composables are placed in Composition given its
                // index position in the for loop
                MovieOverview(movie)
            }
        }
    }
}

private object LifecycleSnippet4 {
    @Composable
    fun MovieOverview(movie: Movie) {
        Column {
            // Side effect explained later in the docs. If MovieOverview
            // recomposes, while fetching the image is in progress,
            // it is cancelled and restarted.
            val image = loadNetworkImage(movie.url)
            MovieHeader(image)

            /* ... */
        }
    }
}

private object LifecycleSnippet5 {
    @Composable
    fun MoviesScreen(movies: List<Movie>) {
        Column {
            for (movie in movies) {
                key(movie.id) { // Unique ID for this movie
                    MovieOverview(movie)
                }
            }
        }
    }
}

private object LifecycleSnippet6 {
    @Composable
    fun MoviesScreen(movies: List<Movie>) {
        LazyColumn {
            items(movies, key = { movie -> movie.id }) { movie ->
                MovieOverview(movie)
            }
        }
    }
}

private object LifecycleSnippet7 {
    // Marking the type as stable to favor skipping and smart recompositions.
    @Stable
    interface UiState<T : Result<T>> {
        val value: T?
        val exception: Throwable?

        val hasError: Boolean
            get() = exception != null
    }
}

/*
Fakes needed for snippets to build:
 */

@Composable
private fun LoginError() { }

@Composable
private fun MovieOverview(movie: Movie) { }
@Composable
private fun MovieHeader(movie: String) { }
private data class Movie(val id: Long, val url: String = "")

private fun loadNetworkImage(url: String): String = ""
