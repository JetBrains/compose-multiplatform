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
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "ClassName"
)

package androidx.compose.integration.docs.adding

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/interop/adding
 *
 * No action required if it's modified.
 */

private object AddingToProjectSnippet1 {

    class MyActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // ...

            val greeting = findViewById<ComposeView>(R.id.greeting)
            greeting.setContent {
                MdcTheme { // or AppCompatTheme
                    Greeting()
                }
            }
        }
    }

    @Composable
    private fun Greeting() {
        Text(
            text = stringResource(R.string.greeting),
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.margin_small))
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

private object AddingToProjectSnippet2 {

    class MyActivityTest {
        @Rule
        @JvmField
        val composeTestRule = createAndroidComposeRule<MyActivity>()

        @Test
        fun testGreeting() {
            val greeting = InstrumentationRegistry.getInstrumentation()
                .targetContext.resources.getString(R.string.greeting)

            composeTestRule.onNodeWithText(greeting).assertIsDisplayed()
        }
    }
}

private object AddingToProjectSnippet3 {

    class MyActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                MyScreen()
            }
        }
    }

    @Composable
    private fun MyScreen(
        viewModel: MyViewModel = viewModel()
    ) {
        val uiState by viewModel.uiState.collectAsState()
        when {
            uiState.isLoading -> { /* ... */ }
            uiState.isSuccess -> { /* ... */ }
            uiState.isError -> { /* ... */ }
        }
    }

    class MyViewModel : ViewModel() {
        private val _uiState = MutableStateFlow(MyScreenState.Loading)
        val uiState: StateFlow<MyScreenState> = _uiState
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object dimen {
        const val margin_small = 1
    }
    object id {
        const val greeting = 2
    }
    object string {
        const val greeting = 3
    }
}

@Composable
private fun MdcTheme(content: @Composable () -> Unit) {
}
private class MyActivity : AppCompatActivity()
private class MyScreenState {
    val isLoading = true
    val isSuccess = true
    val isError = true
    companion object {
        val Loading = MyScreenState()
    }
}
