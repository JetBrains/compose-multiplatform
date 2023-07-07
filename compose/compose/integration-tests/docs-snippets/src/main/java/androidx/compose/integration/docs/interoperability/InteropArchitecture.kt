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
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    class GreetingActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                MaterialTheme {
                    Column {
                        GreetingScreen("user1")
                        GreetingScreen("user2")
                    }
                }
            }
        }
    }

    @Composable
    fun GreetingScreen(
        userId: String,
        viewModel: GreetingViewModel = viewModel(
            factory = GreetingViewModelFactory(userId)
        )
    ) {
        val messageUser by viewModel.message.observeAsState("")

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
    fun MyApp() {
        NavHost(rememberNavController(), startDestination = "profile/{userId}") {
            /* ... */
            composable("profile/{userId}") { backStackEntry ->
                GreetingScreen(backStackEntry.arguments?.getString("userId") ?: "")
            }
        }
    }

    @Composable
    fun GreetingScreen(
        userId: String,
        viewModel: GreetingViewModel = viewModel(
            factory = GreetingViewModelFactory(userId)
        )
    ) {
        val messageUser by viewModel.message.observeAsState("")

        Text(messageUser)
    }
}

private object InteropArchitectureSnippet3 {
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

private class User(val userType: String = "user")
private class FirebaseAnalytics {
    fun setUserProperty(name: String, value: String) {}
}
