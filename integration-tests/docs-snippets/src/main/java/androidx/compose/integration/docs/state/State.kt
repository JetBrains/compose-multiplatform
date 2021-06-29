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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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
    class HelloViewModel : ViewModel() {

        // LiveData holds state which is observed by the UI
        // (state flows down from ViewModel)
        private val _name = MutableLiveData("")
        val name: LiveData<String> = _name

        // onNameChanged is an event we're defining that the UI can invoke
        // (events flow up from UI)
        fun onNameChanged(newName: String) {
            _name.value = newName
        }
    }

    @Composable
    fun HelloScreen(helloViewModel: HelloViewModel = viewModel()) {
        // by default, viewModel() follows the Lifecycle as the Activity or Fragment
        // that calls HelloScreen(). This lifecycle can be modified by callers of HelloScreen.

        // name is the _current_ value of [helloViewModel.name]
        // with an initial value of ""
        val name: String by helloViewModel.name.observeAsState("")

        Column {
            Text(text = name)
            TextField(
                value = name,
                onValueChange = { helloViewModel.onNameChanged(it) },
                label = { Text("Name") }
            )
        }
    }
}

@Composable
private fun StateSnippet6() {
    val nameState: State<String> = helloViewModel.name.observeAsState("")
}

private object StateSnippet7 {
    @Parcelize
    data class City(val name: String, val country: String) : Parcelable

    @Composable
    fun CityScreen() {
        var selectedCity = rememberSaveable {
            mutableStateOf(City("Madrid", "Spain"))
        }
    }
}

private object StateSnippet8 {
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
private fun StateSnippets9() {
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

private lateinit var helloViewModel: StateSnippet5.HelloViewModel

private class HelloViewModel : ViewModel() {

    // LiveData holds state which is observed by the UI
    // (state flows down from ViewModel)
    private val _name = MutableLiveData("")
    val name: LiveData<String> = _name

    // onNameChanged is an event we're defining that the UI can invoke
    // (events flow up from UI)
    fun onNameChanged(newName: String) {
        _name.value = newName
    }
}

/**
 * Add fake Parcelize and Parcelable to avoid adding AndroidX wide dependency on
 * kotlin-parcelize just for snippets
 */
annotation class Parcelize
interface Parcelable