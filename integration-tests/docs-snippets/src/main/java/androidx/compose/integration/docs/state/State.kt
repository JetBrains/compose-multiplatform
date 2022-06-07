/*
 * Copyright 2020 The Android Open Source Project
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
@file:Suppress("unused", "ControlFlowWithEmptyBody", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.state

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/state
 *
 * No action required if it's modified.
 */

private object StateSnippet1 {
    @Composable
    fun HelloContent() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hello!",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.h5
            )
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Name") }
            )
        }
    }
}

private object StateSnippet2 {
    /*
     * MutableState is part of the API, so StateSnippet2 can't have the exact code
     * for the published documentation. Instead, use the following commented code
     * in the doc. If FakeState<T> changes, update the commented code accordingly.
     *
    interface MutableState<T> : State<T> {
        override var value: T
    }
     */
    interface FakeState<T> : State<T> {
        override var value: T
    }

    interface FakeMutableState<T> : MutableState<String>
}

private object StateSnippet3 {
    @Composable
    fun HelloContent() {
        Column(modifier = Modifier.padding(16.dp)) {
            var name by remember { mutableStateOf("") }
            if (name.isNotEmpty()) {
                Text(
                    text = "Hello, $name!",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.h5
                )
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") }
            )
        }
    }
}

private object StateSnippet4 {
    @Composable
    fun HelloScreen() {
        var name by rememberSaveable { mutableStateOf("") }

        HelloContent(name = name, onNameChange = { name = it })
    }

    @Composable
    fun HelloContent(name: String, onNameChange: (String) -> Unit) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hello, $name",
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.h5
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") }
            )
        }
    }
}

private object StateSnippet5 {
    @Parcelize
    data class City(val name: String, val country: String) : Parcelable

    @Composable
    fun CityScreen() {
        var selectedCity = rememberSaveable {
            mutableStateOf(City("Madrid", "Spain"))
        }
    }
}

private object StateSnippet6 {
    data class City(val name: String, val country: String)

    val CitySaver = run {
        val nameKey = "Name"
        val countryKey = "Country"
        mapSaver(
            save = { mapOf(nameKey to it.name, countryKey to it.country) },
            restore = { City(it[nameKey] as String, it[countryKey] as String) }
        )
    }

    @Composable
    fun CityScreen() {
        var selectedCity = rememberSaveable(stateSaver = CitySaver) {
            mutableStateOf(City("Madrid", "Spain"))
        }
    }
}

@Composable
private fun StateSnippets7() {
    data class City(val name: String, val country: String)

    val CitySaver = listSaver<City, Any>(
        save = { listOf(it.name, it.country) },
        restore = { City(it[0] as String, it[1] as String) }
    )

    @Composable
    fun CityScreen() {
        var selectedCity = rememberSaveable(stateSaver = CitySaver) {
            mutableStateOf(City("Madrid", "Spain"))
        }
    }
}

@Composable
private fun StateSnippets8() {
    @Composable
    fun MyApp() {
        MyTheme {
            val scaffoldState = rememberScaffoldState()
            val coroutineScope = rememberCoroutineScope()

            Scaffold(scaffoldState = scaffoldState) { innerPadding ->
                MyContent(
                    showSnackbar = { message ->
                        coroutineScope.launch {
                            scaffoldState.snackbarHostState.showSnackbar(message)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun StateSnippets9() {

    // Plain class that manages App's UI logic and UI elements' state
    class MyAppState(
        val scaffoldState: ScaffoldState,
        val navController: NavHostController,
        private val resources: Resources,
        /* ... */
    ) {
        val bottomBarTabs = /* State */
            // DO NOT COPY IN DAC
            Unit

        // Logic to decide when to show the bottom bar
        val shouldShowBottomBar: Boolean
            get() = /* ... */
                // DO NOT COPY IN DAC
                false

        // Navigation logic, which is a type of UI logic
        fun navigateToBottomBarRoute(route: String) { /* ... */ }

        // Show snackbar using Resources
        fun showSnackbar(message: String) { /* ... */ }
    }

    @Composable
    fun rememberMyAppState(
        scaffoldState: ScaffoldState = rememberScaffoldState(),
        navController: NavHostController = rememberNavController(),
        resources: Resources = LocalContext.current.resources,
        /* ... */
    ) = remember(scaffoldState, navController, resources, /* ... */) {
        MyAppState(scaffoldState, navController, resources, /* ... */)
    }
}

@Composable
private fun StateSnippets10() {
    @Composable
    fun MyApp() {
        MyTheme {
            val myAppState = rememberMyAppState()
            Scaffold(
                scaffoldState = myAppState.scaffoldState,
                bottomBar = {
                    if (myAppState.shouldShowBottomBar) {
                        BottomBar(
                            tabs = myAppState.bottomBarTabs,
                            navigateToRoute = {
                                myAppState.navigateToBottomBarRoute(it)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = myAppState.navController,
                    startDestination = "initial",
                    modifier = Modifier.padding(innerPadding)
                ) { /* ... */ }
            }
        }
    }
}

@Composable
private fun StateSnippets11() {
    data class ExampleUiState(
        val dataToDisplayOnScreen: List<Example> = emptyList(),
        val userMessages: List<Message> = emptyList(),
        val loading: Boolean = false
    )

    class ExampleViewModel(
        private val repository: MyRepository,
        private val savedState: SavedStateHandle
    ) : ViewModel() {

        var uiState by mutableStateOf(ExampleUiState())
            private set

        // Business logic
        fun somethingRelatedToBusinessLogic() { /* ... */ }
    }

    @Composable
    fun ExampleScreen(viewModel: ExampleViewModel = viewModel()) {

        val uiState = viewModel.uiState
        /* ... */

        ExampleReusableComponent(
            someData = uiState.dataToDisplayOnScreen,
            onDoSomething = { viewModel.somethingRelatedToBusinessLogic() }
        )
    }

    @Composable
    fun ExampleReusableComponent(someData: Any, onDoSomething: () -> Unit) {
        /* ... */
        Button(onClick = onDoSomething) {
            Text("Do something")
        }
    }
}

@Composable
private fun StateSnippets12() {
    class ExampleState(
        val lazyListState: LazyListState,
        private val resources: Resources,
        private val expandedItems: List<Item> = emptyList()
    ) {
        fun isExpandedItem(item: Item): Boolean = TODO()
        /* ... */
    }

    @Composable
    fun rememberExampleState(/* ... */): ExampleState { TODO() }

    @Composable
    fun ExampleScreen(viewModel: ExampleViewModel = viewModel()) {

        val uiState = viewModel.uiState
        val exampleState = rememberExampleState()

        LazyColumn(state = exampleState.lazyListState) {
            items(uiState.dataToDisplayOnScreen) { item ->
                if (exampleState.isExpandedItem(item)) {
                    /* ... */
                }
                /* ... */
            }
        }
    }
}

/*
 * Fakes needed for snippets to build:
 */

private object binding {
    object helloText {
        var text = ""
    }

    object textInput {
        fun doAfterTextChanged(function: () -> Unit) {}
    }
}

@Composable
private fun MyTheme(content: @Composable () -> Unit) {}

@Composable
private fun MyContent(showSnackbar: (String) -> Unit, modifier: Modifier = Modifier) {}

@Composable
private fun BottomBar(tabs: Unit, navigateToRoute: (String) -> Unit) {}

@Composable
private fun rememberMyAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    resources: Resources = LocalContext.current.resources
) = remember(scaffoldState, navController, resources) {
    MyAppState(scaffoldState, navController, resources)
}

private class MyAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    private val resources: Resources,
) {
    val shouldShowBottomBar: Boolean = false
    val bottomBarTabs = Unit
    fun navigateToBottomBarRoute(route: String) {}
}

/**
 * Add fake Parcelize and Parcelable to avoid adding AndroidX wide dependency on
 * kotlin-parcelize just for snippets
 */
private annotation class Parcelize
private interface Parcelable

private class Example
private class Item
private class Message
private class MyRepository

@Composable
private fun ExampleReusableComponent(someData: List<Example>, onDoSomething: () -> Unit) {}

private class ExampleViewModel : ViewModel() {
    val uiState = ExampleUiState()
}

private data class ExampleUiState(
    val dataToDisplayOnScreen: List<Item> = emptyList(),
    val userMessages: List<Message> = emptyList(),
    val loading: Boolean = false
)