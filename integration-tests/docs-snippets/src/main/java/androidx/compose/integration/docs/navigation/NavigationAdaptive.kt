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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UnnecessaryLambdaCreation")

package androidx.compose.integration.docs.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/nav-adaptive
 *
 * No action required if it's modified.
 */

private object NavigationAdaptiveSnippet1 {

    enum class WindowSizeClass { Compact, Medium, Expanded }

    @Composable
    fun MyApp(windowSizeClass: WindowSizeClass) {
        // Perform logic on the size class to decide whether to show
        // the nav rail
        val showNavRail = windowSizeClass != WindowSizeClass.Compact
        MyScreen(
            showNavRail = showNavRail,
            /* ... */
        )
    }
}

private object NavigationAdaptiveSnippet2 {

    @Composable
    fun JetnewsNavGraph(
        navController: NavHostController,
        isExpandedScreen: Boolean,
        // ...
    ) {
        // ...
        NavHost(
            navController = navController,
            startDestination = JetnewsDestinations.HomeRoute
        ) {
            composable(JetnewsDestinations.HomeRoute) {
                // ...
                HomeRoute(
                    isExpandedScreen = isExpandedScreen,
                    // ...
                )
            }
            // ...
        }
    }
}

private object NavigationAdaptiveSnippet3 {

    @Composable
    fun HomeRoute(
        // if the window size class is expanded
        isExpandedScreen: Boolean,
        // if the user is focused on the selected article
        isArticleOpen: Boolean,
        // ...
    ) {
        // ...
        if (isExpandedScreen) {
            HomeListWithArticleDetailsScreen(/* ... */)
        } else {
            // if we don't have room for both the list and article details,
            // show one of them based on the user's focus
            if (isArticleOpen) {
                ArticleScreen(/* ... */)
            } else {
                HomeListScreen(/* ... */)
            }
        }
    }
}

private object NavigationAdaptiveSnippet4 {

    class HomeViewModel(/* ... */) {
        fun selectArticle(articleId: String) {
            viewModelState.update {
                it.copy(
                    isArticleOpen = true,
                    selectedArticleId = articleId
                )
            }
        }
    }

    @Composable
    fun HomeRoute(
        isExpandedScreen: Boolean,
        isArticleOpen: Boolean,
        selectedArticleId: String,
        onSelectArticle: (String) -> Unit,
        // ...
    ) {
        // ...
        if (isExpandedScreen) {
            HomeListWithArticleDetailsScreen(
                selectedArticleId = selectedArticleId,
                onSelectArticle = onSelectArticle,
                // ...
            )
        } else {
            // if we don't have room for both the list and article details,
            // show one of them based on the user's focus
            if (isArticleOpen) {
                ArticleScreen(
                    selectedArticleId = selectedArticleId,
                    // ...
                )
            } else {
                HomeListScreen(
                    onSelectArticle = onSelectArticle,
                    // ...
                )
            }
        }
    }
}

private object NavigationAdaptiveSnippet5 {

    class HomeViewModel(/* ... */) {
        fun onArticleBackPress() {
            viewModelState.update {
                it.copy(isArticleOpen = false)
            }
        }
    }

    @Composable
    fun HomeRoute(
        isExpandedScreen: Boolean,
        isArticleOpen: Boolean,
        selectedArticleId: String,
        onSelectArticle: (String) -> Unit,
        onArticleBackPress: () -> Unit,
        // ...
    ) {
        // ...
        if (isExpandedScreen) {
            HomeListWithArticleDetailsScreen(/* ... */)
        } else {
            // if we don't have room for both the list and article details,
            // show one of them based on the user's focus
            if (isArticleOpen) {
                ArticleScreen(
                    selectedArticleId = selectedArticleId,
                    onUpPressed = onArticleBackPress,
                    // ...
                )
                BackHandler {
                    onArticleBackPress()
                }
            } else {
                HomeListScreen(/* ... */)
            }
        }
    }
}

private object NavigationAdaptiveSnippet6 {

    @Composable
    fun HomeRoute(
        // if the window size class is expanded
        isExpandedScreen: Boolean,
        // if the user is focused on the selected article
        isArticleOpen: Boolean,
        selectedArticleId: String,
        // ...
    ) {
        val homeListState = rememberHomeListState()
        val articleState = rememberSaveable(
            selectedArticleId,
            saver = ArticleState.Saver
        ) {
            ArticleState()
        }

        if (isExpandedScreen) {
            HomeListWithArticleDetailsScreen(
                homeListState = homeListState,
                articleState = articleState,
                // ...
            )
        } else {
            // if we don't have room for both the list and article details,
            // show one of them based on the user's focus
            if (isArticleOpen) {
                ArticleScreen(
                    articleState = articleState,
                    // ...
                )
            } else {
                HomeListScreen(
                    homeListState = homeListState,
                    // ...
                )
            }
        }
    }
}

/*
Fakes needed for snippets to build:
 */

@Composable
private fun MyScreen(
    showNavRail: Boolean
) = Unit

private object JetnewsDestinations {
    const val HomeRoute = "home"
    const val InterestsRoute = "interests"
}

@Composable
private fun HomeRoute(
    isExpandedScreen: Boolean
) = Unit

@Composable
private fun HomeListWithArticleDetailsScreen(
    selectedArticleId: String = TODO(),
    onSelectArticle: (String) -> Unit = TODO(),
    homeListState: HomeListState = TODO(),
    articleState: ArticleState = TODO()
) = Unit

@Composable
private fun ArticleScreen(
    selectedArticleId: String = TODO(),
    onUpPressed: () -> Unit = TODO(),
    articleState: ArticleState = TODO()
) = Unit

@Composable
private fun HomeListScreen(
    onSelectArticle: (String) -> Unit = TODO(),
    homeListState: HomeListState = TODO()
) = Unit

@Composable
private fun rememberHomeListState(): HomeListState = TODO()

private class HomeListState

private class ArticleState {
    companion object {
        val Saver: Saver<ArticleState, *> = TODO()
    }
}

data class HomeViewModelState(
    val selectedArticleId: String,
    val isArticleOpen: Boolean,
)

val viewModelState: MutableStateFlow<HomeViewModelState> = TODO()
