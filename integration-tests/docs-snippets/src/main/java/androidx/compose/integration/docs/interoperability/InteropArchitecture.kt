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

// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
// Ignore lint warnings in documentation snippets
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER",
    "RedundantSuspendModifier", "CascadeIf", "ClassName", "RemoveExplicitTypeArguments",
    "ControlFlowWithEmptyBody", "PropertyName", "CanBeParameter"
)

package androidx.compose.integration.docs.interoperability

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/interop/compose-in-existing-arch
 *
 * No action required if it's modified.
 */

private object InteropArchitectureSnippet1 {
    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                MaterialTheme {
                    Column {
                        Greeting("user1")
                        Greeting("user2")
                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(userId: String) {
        val greetingViewModel: GreetingViewModel = viewModel(
            factory = GreetingViewModelFactory(userId)
        )
        val messageUser by greetingViewModel.message.observeAsState("")

        Text(messageUser)
    }

    class GreetingViewModel(private val userId: String) : ViewModel() {
        private val _message = MutableLiveData("Hi $userId")
        val message: LiveData<String> = _message
    }

    class GreetingViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GreetingViewModel(userId) as T
        }
    }
}

private object InteropArchitectureSnippet2 {
    @Composable
    fun MyScreen() {
        NavHost(rememberNavController(), startDestination = "profile/{userId}") {
            /* ... */
            composable("profile/{userId}") { backStackEntry ->
                Greeting(backStackEntry.arguments?.getString("userId") ?: "")
            }
        }
    }

    @Composable
    fun Greeting(userId: String) {
        val greetingViewModel: GreetingViewModel = viewModel(
            factory = GreetingViewModelFactory(userId)
        )
        val messageUser by greetingViewModel.message.observeAsState("")

        Text(messageUser)
    }
}

private object InteropArchitectureSnippet3 {
    @Composable
    fun BackHandler(
        enabled: Boolean,
        backDispatcher: OnBackPressedDispatcher,
        onBack: () -> Unit
    ) {

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

        // On every successful composition, update the callback with the `enabled` value
        // to tell `backCallback` whether back events should be intercepted or not
        SideEffect {
            backCallback.isEnabled = enabled
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

private object InteropArchitectureSnippet4 {
    class CustomViewGroup @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : LinearLayout(context, attrs, defStyle) {

        // Source of truth in the View system as mutableStateOf
        // to make it thread-safe for Compose
        private var text by mutableStateOf("")

        private val textView: TextView

        init {
            orientation = VERTICAL

            textView = TextView(context)
            val composeView = ComposeView(context).apply {
                setContent {
                    MaterialTheme {
                        TextField(value = text, onValueChange = { updateState(it) })
                    }
                }
            }

            addView(textView)
            addView(composeView)
        }

        // Update both the source of truth and the TextView
        private fun updateState(newValue: String) {
            text = newValue
            textView.text = newValue
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private class GreetingViewModel(userId: String) : ViewModel() {
    val _message = MutableLiveData("")
    val message: LiveData<String> = _message
}
private class GreetingViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GreetingViewModel(userId) as T
    }
}
