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
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
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

private object LibrariesSnippetActivityResult {
    @Composable
    fun GetContentExample() {
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        val launcher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
            imageUri = uri
        }
        Column {
            Button(onClick = { launcher.launch("image/*") }) {
                Text(text = "Load Image")
            }
            Image(
                painter = rememberImagePainter(imageUri),
                contentDescription = "My Image"
            )
        }
    }
}

@Composable
private fun LibrariesSnippetBackHandler() {
    var backHandlingEnabled by remember { mutableStateOf(true) }
    BackHandler(backHandlingEnabled) {
        // Handle back press
    }
}

private object LibrariesSnippetAddingViewModel {
    class MyViewModel : ViewModel() { /*...*/ }

    @Composable
    fun MyScreen(
        viewModel: MyViewModel = viewModel()
    ) {
        // use viewModel here
    }
}

private object LibrariesSnippetSameViewModelTwice {
    @Composable
    fun MyScreen(
        // Returns the same instance as long as the activity is alive,
        // just as if you grabbed the instance from an Activity or Fragment
        viewModel: MyViewModel = viewModel()
    ) { /* ... */ }

    @Composable
    fun MyScreen2(
        viewModel: MyViewModel = viewModel() // Same instance as in MyExample
    ) { /* ... */ }
}

private object LibrariesSnippetRecomposesWhenStateChanges {
    @Composable
    fun MyScreen(
        viewModel: MyViewModel = viewModel()
    ) {
        val dataExample = viewModel.exampleLiveData.observeAsState()

        // Because the state is read here,
        // MyExample recomposes whenever dataExample changes.
        dataExample.value?.let {
            ShowData(dataExample)
        }
    }
}

private object LibrariesSnippetHilt {
    @HiltViewModel
    class MyViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExampleRepository
    ) : ViewModel() { /* ... */ }

    @Composable
    fun MyScreen(
        viewModel: MyViewModel = viewModel()
    ) { /* ... */ }
}

private object LibrariesSnippetHiltViewModel {
    // import androidx.hilt.navigation.compose.hiltViewModel

    @Composable
    fun MyApp() {
        NavHost(navController, startDestination = startRoute) {
            composable("example") { backStackEntry ->
                // Creates a ViewModel from the current BackStackEntry
                // Available in the androidx.hilt:hilt-navigation-compose artifact
                val viewModel = hiltViewModel<MyViewModel>()
                MyScreen(viewModel)
            }
            /* ... */
        }
    }
}

private object LibrariesSnippetBackStackEntry {
    // import androidx.hilt.navigation.compose.hiltViewModel
    // import androidx.navigation.compose.getBackStackEntry

    @Composable
    fun MyApp() {
        NavHost(navController, startDestination = startRoute) {
            navigation(startDestination = innerStartRoute, route = "Parent") {
                // ...
                composable("exampleWithRoute") { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry("Parent")
                    }
                    val parentViewModel = hiltViewModel<ParentViewModel>(
                        parentEntry
                    )
                    ExampleWithRouteScreen(parentViewModel)
                }
            }
        }
    }
}

private object LibrariesSnippetPaging {
    @Composable
    fun MyScreen(flow: Flow<PagingData<String>>) {
        val lazyPagingItems = flow.collectAsLazyPagingItems()
        LazyColumn {
            items(lazyPagingItems) {
                Text("Item is $it")
            }
        }
    }
}

private object LibrariesSnippetRemoteImages {
    @Composable
    fun MyScreen() {
        val painter = rememberImagePainter(
            data = "https://picsum.photos/300/300",
            builder = {
                crossfade(true)
            }
        )

        Box {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.image_content_desc),
            )

            when (painter.state) {
                is ImagePainter.State.Loading -> {
                    // Display a circular progress indicator whilst loading
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is ImagePainter.State.Error -> {
                    // If you wish to display some content if the request fails
                }
            }
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object drawable {
        const val ic_error = 1
    }
    object string {
        const val image_content_desc = 2
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
private class MyViewModel : ViewModel() {
    val exampleLiveData = MutableLiveData(" ")
}

private inline fun <reified VM : ViewModel> hiltViewModel(): VM { TODO() }
private inline fun <reified VM : ViewModel> hiltViewModel(backStackEntry: NavBackStackEntry): VM {
    TODO()
}

@Composable
private fun MyScreen(vm: MyViewModel) {
    TODO()
}

@Composable
private fun ExampleWithRouteScreen(vm: ParentViewModel) {
    TODO()
}

private val navController: NavHostController = TODO()
private val innerStartRoute: String = TODO()
private val startRoute: String = TODO()

private class PagingData<T>

private fun Flow<PagingData<String>>.collectAsLazyPagingItems() = listOf("")

// Coil
interface ImageRequest { interface Builder }

@Composable
fun rememberImagePainter(
    data: Any?,
    builder: ImageRequest.Builder.() -> Unit = {},
): LoadPainter { TODO() }
fun ImageRequest.Builder.crossfade(enable: Boolean): Nothing = TODO()

fun interface Loader<R> {
    fun load(request: R, size: IntSize): Flow<ImagePainter.State>
}
abstract class LoadPainter : Painter() {
    var state: ImagePainter.State by mutableStateOf(ImagePainter.State.Loading)
        private set
}
interface ImagePainter {
    sealed class State {
        object Loading : State()
        object Error : State()
    }
}
