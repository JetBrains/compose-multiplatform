// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER",
    "RedundantSuspendModifier", "CascadeIf", "ClassName", "SameParameterValue"
)

package androidx.compose.integration.docs.libraries

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.coroutines.flow.Flow

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/libraries
 *
 * No action required if it's modified.
 */

private object LibrariesSnippet1 {
    class ExampleViewModel : ViewModel() { /*...*/ }

    @Composable
    fun MyExample(
        viewModel: ExampleViewModel = viewModel()
    ) {
        // use viewModel here
    }
}

private object LibrariesSnippet2 {
    @Composable
    fun MyExample(
        // Returns the same instance as long as the activity is alive,
        // just as if you grabbed the instance from an Activity or Fragment
        viewModel: ExampleViewModel = viewModel()
    ) { /* ... */ }

    @Composable
    fun MyExample2(
        viewModel: ExampleViewModel = viewModel() // Same instance as in MyExample
    ) { /* ... */ }
}

private object LibrariesSnippet3 {
    @Composable
    fun MyExample(
        viewModel: ExampleViewModel = viewModel()
    ) {
        val dataExample = viewModel.exampleLiveData.observeAsState()

        // Because the state is read here,
        // MyExample recomposes whenever dataExample changes.
        dataExample.value?.let {
            ShowData(dataExample)
        }
    }
}

private object LibrariesSnippet4 {
    @HiltViewModel
    class ExampleViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExampleRepository
    ) : ViewModel() { /* ... */ }

    @Composable
    fun ExampleScreen(
        exampleViewModel: ExampleViewModel = viewModel()
    ) { /* ... */ }
}

private object LibrariesSnippet5 {
    // import androidx.hilt.navigation.compose.hiltViewModel

    @Composable
    fun MyApp() {
        NavHost(navController, startDestination = startRoute) {
            composable("example") { backStackEntry ->
                // Creates a ViewModel from the current BackStackEntry
                // Available in the androidx.hilt:hilt-navigation-compose artifact
                val exampleViewModel = hiltViewModel<ExampleViewModel>()
                ExampleScreen(exampleViewModel)
            }
            /* ... */
        }
    }
}

private object LibrariesSnippet6 {
    // import androidx.hilt.navigation.compose.hiltViewModel
    // import androidx.navigation.compose.getBackStackEntry

    @Composable
    fun MyApp() {
        NavHost(navController, startDestination = startRoute) {
            navigation(startDestination = innerStartRoute, route = "Parent") {
                // ...
                composable("exampleWithRoute") { backStackEntry ->
                    val parentViewModel = hiltViewModel<ParentViewModel>(
                        navController.getBackStackEntry("Parent")
                    )
                    ExampleWithRouteScreen(parentViewModel)
                }
            }
        }
    }
}

private object LibrariesSnippet7 {
    @Composable
    fun MyExample(flow: Flow<PagingData<String>>) {
        val lazyPagingItems = flow.collectAsLazyPagingItems()
        LazyColumn {
            items(lazyPagingItems) {
                Text("Item is $it")
            }
        }
    }
}

private object LibrariesSnippet8 {
    @Composable
    fun MyExample() {
        CoilImage(
            data = "https://picsum.photos/300/300",
            loading = {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            },
            error = {
                Image(painterResource(R.drawable.ic_error), contentDescription = "Error")
            }
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object drawable {
        const val ic_error = 1
    }
}

private fun ShowData(dataExample: State<String?>): Nothing = TODO()
private class ExampleImageLoader {
    fun load(url: String): DummyInto = TODO()
    fun cancel(listener: Listener): Any = TODO()

    open class Listener {
        open fun onSuccess(bitmap: Bitmap): Unit = TODO()
    }

    companion object {
        fun get() = ExampleImageLoader()
    }
}

private class DummyInto {
    fun into(listener: ExampleImageLoader.Listener) {}
}

private class SavedStateHandle
private class ExampleRepository
private annotation class HiltViewModel
private annotation class Inject

private class ParentViewModel : ViewModel()
private class ExampleViewModel : ViewModel() {
    val exampleLiveData = MutableLiveData(" ")
}

private inline fun <reified VM : ViewModel> hiltViewModel(): VM { TODO() }
private inline fun <reified VM : ViewModel> hiltViewModel(backStackEntry: NavBackStackEntry): VM {
    TODO()
}

@Composable
private fun ExampleScreen(vm: ExampleViewModel) {
    TODO()
}

@Composable
private fun ExampleWithRouteScreen(vm: ParentViewModel) {
    TODO()
}

@Composable
private fun CoilImage(
    data: String,
    error: @Composable () -> Unit,
    loading: @Composable () -> Unit
) {
    TODO()
}

private val navController: NavHostController = TODO()
private val innerStartRoute: String = TODO()
private val startRoute: String = TODO()

private class PagingData<T>

private fun Flow<PagingData<String>>.collectAsLazyPagingItems() = listOf("")