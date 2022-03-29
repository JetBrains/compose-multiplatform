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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.Boolean
import kotlin.Exception
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.random.Random

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/side-effects
 *
 * No action required if it's modified.
 */

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
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

        Scaffold(scaffoldState = scaffoldState) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
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
    fun HomeScreen(
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        onStart: () -> Unit, // Send the 'started' analytics event
        onStop: () -> Unit // Send the 'stopped' analytics event
    ) {
        // Safely update the current lambdas when a new one is provided
        val currentOnStart by rememberUpdatedState(onStart)
        val currentOnStop by rememberUpdatedState(onStop)

        // If `lifecycleOwner` changes, dispose and reset the effect
        DisposableEffect(lifecycleOwner) {
            // Create an observer that triggers our remembered callbacks
            // for sending analytics events
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    currentOnStart()
                } else if (event == Lifecycle.Event.ON_STOP) {
                    currentOnStop()
                }
            }

            // Add the observer to the lifecycle
            lifecycleOwner.lifecycle.addObserver(observer)

            // When the effect leaves the Composition, remove the observer
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        /* Home screen content */
    }
}

private object SideEffectsSnippet5 {
    @Composable
    fun rememberAnalytics(user: User): FirebaseAnalytics {
        val analytics: FirebaseAnalytics = remember {
            // START - DO NOT COPY IN CODE SNIPPET
            FirebaseAnalytics()
            // END - DO NOT COPY IN CODE SNIPPET, just use /* ... */
        }

        // On every successful composition, update FirebaseAnalytics with
        // the userType from the current User, ensuring that future analytics
        // events have this metadata attached
        SideEffect {
            analytics.setUserProperty("userType", user.userType)
        }
        return analytics
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
        return produceState<Result<Image>>(initialValue = Result.Loading, url, imageRepository) {

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
        val highPriorityTasks by remember(highPriorityKeywords) {
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
    fun HomeScreen(
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        onStart: () -> Unit, // Send the 'started' analytics event
        onStop: () -> Unit // Send the 'stopped' analytics event
    ) {
        // These values never change in Composition
        val currentOnStart by rememberUpdatedState(onStart)
        val currentOnStop by rememberUpdatedState(onStop)

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                // START - DO NOT COPY IN CODE SNIPPET
                if (event == Lifecycle.Event.ON_START) {
                    currentOnStart()
                } else if (event == Lifecycle.Event.ON_STOP) {
                    currentOnStop()
                }
                // END - DO NOT COPY IN CODE SNIPPET, just use /* ... */
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
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

private class FirebaseAnalytics {
    fun setUserProperty(name: String, value: String) {}
}

private sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    object Loading : Result<Nothing>()
    object Error : Result<Nothing>()
}

private class User(val userType: String = "user")
private class Weather
private class Greeting(val name: String)
private fun prepareGreeting(user: User, weather: Weather) = Greeting("haha")

private fun String.containsWord(input: List<String>): Boolean = false

private object MyAnalyticsService {
    fun sendScrolledPastFirstItemEvent() = Unit
}
