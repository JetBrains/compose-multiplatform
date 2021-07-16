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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "ClassName")

package androidx.compose.integration.docs.compositionlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * This file lets DevRel track changes to the CompositionLocal code snippets present in
 * https://developer.android.com/jetpack/compose/compositionlocal
 *
 * No action required if it's modified.
 */

private object CompositionLocalSnippet1 {

    @Composable
    fun MyApp() {
        // Theme information tends to be defined near the root of the application
//      val colors = ...
        // TODO: COPY LINE ABOVE INTO THE CODE SNIPPETS
    }

    // Some composable deep in the hierarchy
    @Composable
    fun SomeTextLabel(labelText: String) {
        Text(
            text = labelText,
//          color = // ← need to access colors here
            color = Color.Black // TODO: COPY LINE ABOVE INTO THE CODE SNIPPETS
        )
    }
}

private object CompositionLocalSnippet2 {
    @Composable
    fun MyApp() {
        // Provides a Theme whose values are propagated down its `content`
        MaterialTheme {
            // New values for colors, typography, and shapes are available
            // in MaterialTheme's content lambda.

            // ... content here ...
        }
    }

    // Some composable deep in the hierarchy of MaterialTheme
    @Composable
    fun SomeTextLabel(labelText: String) {
        Text(
            text = labelText,
            // `primaryColor` is obtained from MaterialTheme's
            // LocalColors CompositionLocal
            color = MaterialTheme.colors.primary
        )
    }
}

private object CompositionLocalSnippet3 {
    @Composable
    fun CompositionLocalExample() {
        MaterialTheme { // MaterialTheme sets ContentAlpha.high as default
            Column {
                Text("Uses MaterialTheme's provided alpha")
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text("Medium value provided for LocalContentAlpha")
                    Text("This Text also uses the medium value")
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                        DescendantExample()
                    }
                }
            }
        }
    }

    @Composable
    fun DescendantExample() {
        // CompositionLocalProviders also work across composable functions
        Text("This Text uses the disabled alpha now")
    }
}

private object CompositionLocalSnippet4 {
    @Composable
    fun FruitText(fruitSize: Int) {
        // Get `resources` from the current value of LocalContext
        val resources = LocalContext.current.resources
        val fruitText = remember(resources, fruitSize) {
            resources.getQuantityString(R.plurals.fruit_title, fruitSize)
        }
        Text(text = fruitText)
    }
}

private object CompositionLocalSnippet5and6and7 {
    // -------- CODE SNIPPET 5 --------

    // LocalElevations.kt file

    data class Elevations(val card: Dp = 0.dp, val default: Dp = 0.dp)

    // Define a CompositionLocal global object with a default
    // This instance can be accessed by all composables in the app
    val LocalElevations = compositionLocalOf { Elevations() }

    // -------- CODE SNIPPET 6 --------

    // MyActivity.kt file

    class MyActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                // Calculate elevations based on the system theme
                val elevations = if (isSystemInDarkTheme()) {
                    Elevations(card = 1.dp, default = 1.dp)
                } else {
                    Elevations(card = 0.dp, default = 0.dp)
                }

                // Bind elevation as the value for LocalElevations
                CompositionLocalProvider(LocalElevations provides elevations) {
                    // ... Content goes here ...
                    // This part of Composition will see the `elevations` instance
                    // when accessing LocalElevations.current
                }
            }
        }
    }

    // -------- CODE SNIPPET 7 --------

    @Composable
    fun SomeComposable() {
        // Access the globally defined LocalElevations variable to get the
        // current Elevations in this part of the Composition
        Card(elevation = LocalElevations.current.card) {
            // Content
        }
    }
}

private object CompositionLocalSnippet8 {
    @Composable
    fun MyComposable(myViewModel: MyViewModel = viewModel()) {
        // ...
        MyDescendant(myViewModel.data)
    }

    // Don't pass the whole object! Just what the descendant needs.
    // Also, don't  pass the ViewModel as an implicit dependency using
    // a CompositionLocal.
    @Composable
    fun MyDescendant(myViewModel: MyViewModel) { /* ... */ }

    // Pass only what the descendant needs
    @Composable
    fun MyDescendant(data: DataToDisplay) {
        // Display data
    }
}

private object CompositionLocalSnippet9 {
    @Composable
    fun MyComposable(myViewModel: MyViewModel = viewModel()) {
        // ...
        MyDescendant(myViewModel)
    }

    @Composable
    fun MyDescendant(myViewModel: MyViewModel) {
        Button(onClick = { myViewModel.loadData() }) {
            Text("Load data")
        }
    }
}

private object CompositionLocalSnippet10 {
    @Composable
    fun MyComposable(myViewModel: MyViewModel = viewModel()) {
        // ...
        ReusableLoadDataButton(
            onLoadClick = {
                myViewModel.loadData()
            }
        )
    }

    @Composable
    fun ReusableLoadDataButton(onLoadClick: () -> Unit) {
        Button(onClick = onLoadClick) {
            Text("Load data")
        }
    }
}

private object CompositionLocalSnippet11 {
    @Composable
    fun MyComposable(myViewModel: MyViewModel = viewModel()) {
        // ...
        ReusablePartOfTheScreen(
            content = {
                Button(
                    onClick = {
                        myViewModel.loadData()
                    }
                ) {
                    Text("Confirm")
                }
            }
        )
    }

    @Composable
    fun ReusablePartOfTheScreen(content: @Composable () -> Unit) {
        Column {
            // ...
            content()
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object plurals {
        const val fruit_title = 1
    }
}

private class DataToDisplay
private class MyViewModel : ViewModel() {
    val data = DataToDisplay()
    fun loadData() { }
}
