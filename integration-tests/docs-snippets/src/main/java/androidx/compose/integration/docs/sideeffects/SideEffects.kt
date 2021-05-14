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

package androidx.compose.integration.docs.sideeffects

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/side-effects
 *
 * No action required if it's modified.
 */

@ExperimentalMaterialApi
private object SideEffectsSnippet1 {
    @Composable
    fun MyScreen(
        state: UiState<List<Movie>>,
        scaffoldState: ScaffoldState = rememberScaffoldState()
    ) {

        // If the UI state contains an error, show snackbar
        if (state.hasError) {

            // `LaunchedEffect` will cancel and re-launch if
            // `scaffoldState.snackbarHostState` changes
            LaunchedEffect(scaffoldState.snackbarHostState) {
                // Show snackbar using a coroutine, when the coroutine is cancelled the
                // snackbar will automatically dismiss. This coroutine will cancel whenever
                // `state.hasError` is false, and only start when `state.hasError` is true
                // (due to the above if-check), or if `scaffoldState.snackbarHostState` changes.
                scaffoldState.snackbarHostState.showSnackbar(
                    message = "Error message",
                    actionLabel = "Retry message"
                )
            }
        }

        Scaffold(scaffoldState = scaffoldState) {
            /* ... */
        }
    }
}

@ExperimentalMaterialApi
private object SideEffectsSnippet2 {
    @Composable
    fun MoviesScreen(scaffoldState: ScaffoldState = rememberScaffoldState()) {

        // Creates a CoroutineScope bound to the MoviesScreen's lifecycle
        val scope = rememberCoroutineScope()

        Scaffold(scaffoldState = scaffoldState) {
            Column {
                /* ... */
                Button(
                    onClick = {
                        // Create a new coroutine in the event handler to show a snackbar
                        scope.launch {
                            scaffoldState.snackbarHostState.showSnackbar("Something happened!")
                        }
                    }
                ) {
                    Text("Press me")
                }
            }
        }
    }
}

private object SideEffectsSnippet3 {
    @Composable
    fun LandingScreen(onTimeout: () -> Unit) {

        // This will always refer to the latest onTimeout function that
        // LandingScreen was recomposed with
        val currentOnTimeout by rememberUpdatedState(onTimeout)

        // Create an effect that matches the lifecycle of LandingScreen.
        // If LandingScreen recomposes, the delay shouldn't start again.
        LaunchedEffect(true) {
            delay(SplashWaitTimeMillis)
            currentOnTimeout()
        }

        /* Landing screen content */
    }
}

private object SideEffectsSnippet4 {
    @Composable
    fun BackHandler(backDispatcher: OnBackPressedDispatcher, onBack: () -> Unit) {

        // Safely update the current `onBack` lambda when a new one is provided
        val currentOnBack by rememberUpdatedState(onBack)

        // Remember in Composition a back callback that calls the `onBack` lambda
        val backCallback = remember {
            // Always intercept back events. See the SideEffect for a more complete version
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }

        // If `backDispatcher` changes, dispose and reset the effect
        DisposableEffect(backDispatcher) {
            // Add callback to the backDispatcher
            backDispatcher.addCallback(backCallback)

            // When the effect leaves the Composition, remove the callback
            onDispose {
                backCallback.remove()
            }
        }
    }
}

private object SideEffectsSnippet5 {
    @Composable
    fun BackHandler(
        backDispatcher: OnBackPressedDispatcher,
        enabled: Boolean = true, // Whether back events should be intercepted or not
        onBack: () -> Unit
    ) {
        // START - DO NOT COPY IN CODE SNIPPET
        val currentOnBack by rememberUpdatedState(onBack)

        val backCallback = remember {
            // Always intercept back events. See the SideEffect for a more complete version
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }
        // END - DO NOT COPY IN CODE SNIPPET, just use /* ... */

        // On every successful composition, update the callback with the `enabled` value
        // to tell `backCallback` whether back events should be intercepted or not
        SideEffect {
            backCallback.isEnabled = enabled
        }

        /* Rest of the code */
    }
}

private object SideEffectsSnippet6 {
    @Composable
    fun loadNetworkImage(
        url: String,
        imageRepository: ImageRepository
    ): State<Result<Image>> {

        // Creates a State<T> with Result.Loading as initial value
        // If either `url` or `imageRepository` changes, the running producer
        // will cancel and will be re-launched with the new inputs.
        return produceState(initialValue = Result.Loading, url, imageRepository) {

            // In a coroutine, can make suspend calls
            val image = imageRepository.load(url)

            // Update State with either an Error or Success result.
            // This will trigger a recomposition where this State is read
            value = if (image == null) {
                Result.Error
            } else {
                Result.Success(image)
            }
        }
    }
}

private object SideEffectsSnippet7 {
    @Composable
    fun TodoList(highPriorityKeywords: List<String> = listOf("Review", "Unblock", "Compose")) {

        val todoTasks = remember { mutableStateListOf<String>() }

        // Calculate high priority tasks only when the todoTasks or highPriorityKeywords
        // change, not on every recomposition
        val highPriorityTasks by remember(todoTasks, highPriorityKeywords) {
            derivedStateOf { todoTasks.filter { it.containsWord(highPriorityKeywords) } }
        }

        Box(Modifier.fillMaxSize()) {
            LazyColumn {
                items(highPriorityTasks) { /* ... */ }
                items(todoTasks) { /* ... */ }
            }
            /* Rest of the UI where users can add elements to the list */
        }
    }
}

@Composable
private fun SideEffectsSnippet8(messages: List<Message>) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        // ...
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it == true }
            .collect {
                MyAnalyticsService.sendScrolledPastFirstItemEvent()
            }
    }
}

private object SideEffectsSnippet9 {
    @Composable
    fun BackHandler(backDispatcher: OnBackPressedDispatcher, onBack: () -> Unit) {
        // START - DO NOT COPY IN CODE SNIPPET
        val currentOnBack by rememberUpdatedState(onBack)

        val backCallback = remember {
            // Always intercept back events. See the SideEffect for a more complete version
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }
        // END - DO NOT COPY IN CODE SNIPPET, just use /* ... */

        DisposableEffect(backDispatcher) {
            backDispatcher.addCallback(backCallback)
            onDispose {
                backCallback.remove()
            }
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private const val SplashWaitTimeMillis = 1000L

private data class Movie(val id: Long, val url: String = "")

private data class UiState<T>(
    val loading: Boolean = false,
    val exception: Exception? = null,
    val data: T? = null
) {
    val hasError: Boolean
        get() = exception != null
}

private class Message(val id: Long)
private class Image
private class ImageRepository {
    fun load(url: String): Image? = if (Random.nextInt() == 0) Image() else null // Avoid warnings
}

private sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    object Loading : Result<Nothing>()
    object Error : Result<Nothing>()
}

private class User
private class Weather
private class Greeting(val name: String)
private fun prepareGreeting(user: User, weather: Weather) = Greeting("haha")

private fun String.containsWord(input: List<String>): Boolean = false

private object MyAnalyticsService {
    fun sendScrolledPastFirstItemEvent() = Unit
}
