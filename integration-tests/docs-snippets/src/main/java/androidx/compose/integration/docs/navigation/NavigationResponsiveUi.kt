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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UnnecessaryLambdaCreation")

package androidx.compose.integration.docs.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/guide/topics/large-screens/navigation-for-responsive-uis.
 *
 * No action required if it's modified.
 */

private object NavigationResponsiveUiSnippet1 {

  enum class WindowSizeClass { Compact, Medium, Expanded }

  @Composable
  fun MyApp(windowSizeClass: WindowSizeClass) {
    // Select a navigation element based on display size.
    when (windowSizeClass) {
      WindowSizeClass.Compact -> { /* Bottom bar */ }
      WindowSizeClass.Medium -> { /* Rail */ }
      WindowSizeClass.Expanded -> { /* Persistent drawer */ }
    }
  }
}

private object NavigationResponsiveUiSnippet2 {

  /* Displays a list of items. */
  @Composable
  fun ListOfItems(
      onItemSelected: (String) -> Unit,
  ) { /*...*/ }

  /* Displays the detail for an item. */
  @Composable
  fun ItemDetail(
      selectedItemId: String? = null,
  ) { /*...*/ }

  /* Displays a list and the detail for an item side by side. */
  @Composable
  fun ListAndDetail(
      selectedItemId: String? = null,
      onItemSelected: (String) -> Unit,
  ) {
    Row {
      ListOfItems(onItemSelected = onItemSelected)
      ItemDetail(selectedItemId = selectedItemId)
    }
  }
}

private object NavigationResponsiveUiSnippet3 {

  @Composable
  fun ListDetailRoute(
      // Indicates that the display size is represented by the expanded window size class.
      isExpandedWindowSize: Boolean = false,
      // Identifies the item selected from the list. If null, a item has not been selected.
      selectedItemId: String?,
  ) {
    if (isExpandedWindowSize) {
      ListAndDetail(
        selectedItemId = selectedItemId,
        /*...*/
      )
    } else {
      // If the display size cannot accommodate both the list and the item detail,
      // show one of them based on the user's focus.
      if (selectedItemId != null) {
        ItemDetail(
          selectedItemId = selectedItemId,
          /*...*/
        )
      } else {
        ListOfItems(/*...*/)
      }
    }
  }
}

@Composable
private fun NavigationResponsiveUiSnippet4(
    navController: NavHostController = rememberNavController(),
    isExpandedWindowSize: Boolean,
    selectedItemId: String?,
) {
    NavHost(navController = navController, startDestination = "listDetailRoute") {
      composable("listDetailRoute") {
        ListDetailRoute(isExpandedWindowSize = isExpandedWindowSize,
                        selectedItemId = selectedItemId)
      }
      /*...*/
    }
}

private object NavigationResponsiveUiSnippet5 {

  class ListDetailViewModel : ViewModel() {

    data class ListDetailUiState(
        val selectedItemId: String? = null,
    )

    private val viewModelState = MutableStateFlow(ListDetailUiState())

    fun onItemSelected(itemId: String) {
      viewModelState.update {
        it.copy(selectedItemId = itemId)
      }
    }
  }

  val listDetailViewModel = ListDetailViewModel()

  @Composable
  fun ListDetailRoute(
      isExpandedWindowSize: Boolean = false,
      selectedItemId: String?,
      onItemSelected: (String) -> Unit = { listDetailViewModel.onItemSelected(it) },
  ) {
    if (isExpandedWindowSize) {
      ListAndDetail(
        selectedItemId = selectedItemId,
        onItemSelected = onItemSelected,
        /*...*/
      )
    } else {
      if (selectedItemId != null) {
        ItemDetail(
          selectedItemId = selectedItemId,
          /*...*/
        )
      } else {
        ListOfItems(
          onItemSelected = onItemSelected,
          /*...*/
        )
      }
    }
  }
}

private object NavigationResponsiveUiSnippet6 {

  class ListDetailViewModel : ViewModel() {

    data class ListDetailUiState(
        val selectedItemId: String? = null,
    )

    private val viewModelState = MutableStateFlow(ListDetailUiState())

    fun onItemSelected(itemId: String) {
      viewModelState.update {
        it.copy(selectedItemId = itemId)
      }
    }

    fun onItemBackPress() {
      viewModelState.update {
        it.copy(selectedItemId = null)
      }
    }
  }

  val listDetailViewModel = ListDetailViewModel()

  @Composable
  fun ListDetailRoute(
      isExpandedWindowSize: Boolean = false,
      selectedItemId: String?,
      onItemSelected: (String) -> Unit = { listDetailViewModel.onItemSelected(it) },
      onItemBackPress: () -> Unit = { listDetailViewModel.onItemBackPress() },
  ) {
    if (isExpandedWindowSize) {
      ListAndDetail(
        selectedItemId = selectedItemId,
        onItemSelected = onItemSelected,
        /*...*/
      )
    } else {
      if (selectedItemId != null) {
        ItemDetail(
          selectedItemId = selectedItemId,
          /*...*/
        )
        BackHandler {
          onItemBackPress()
        }
      } else {
        ListOfItems(
          onItemSelected = onItemSelected,
          /*...*/
        )
      }
    }
  }
}

@Composable
private fun NavigationResponsiveUiSnippet7(
    navController: NavHostController = rememberNavController(),
    isExpandedWindowSize: Boolean,
    selectedItemId: String?,
) {
    NavHost(navController = navController, startDestination = "listDetailRoute") {
      composable("listDetailRoute") {
        ListDetailRoute(isExpandedWindowSize = isExpandedWindowSize,
                        selectedItemId = selectedItemId)
      }
      navigation(startDestination = "itemSubdetail1", route = "itemSubDetail") {
        composable("itemSubdetail1") { ItemSubdetail1(/*...*/) }
        composable("itemSubdetail2") { ItemSubdetail2(/*...*/) }
        composable("itemSubdetail3") { ItemSubdetail3(/*...*/) }
      }
      /*...*/
    }
}

private object NavigationResponsiveUiSnippet8 {

  @Composable
  fun ListDetailRoute(
      // Indicates that the display size is represented by the expanded window size class.
      isExpandedWindowSize: Boolean = false,
      // Identifies the item selected from the list. If null, a item has not been selected.
      selectedItemId: String?,
  ) { /*...*/ }
}

/* Fakes needed for snippets to build. */

@Composable
private fun ListOfItems() = Unit

@Composable
private fun ListOfItems(
    onItemSelected: (String) -> Unit,
) = Unit

@Composable
private fun ItemDetail(
    selectedItemId: String? = null,
) = Unit

@Composable
private fun ListAndDetail(
    selectedItemId: String? = null,
) = Unit

@Composable
private fun ListAndDetail(
    selectedItemId: String? = null,
    onItemSelected: (String) -> Unit,
) = Unit

@Composable
private fun ListDetailRoute(
    isExpandedWindowSize: Boolean = false,
    selectedItemId: String?,
) = Unit

@Composable
private fun ItemSubdetail1() = Unit

@Composable
private fun ItemSubdetail2() = Unit

@Composable
private fun ItemSubdetail3() = Unit

private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {}
