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

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/state
 *
 * No action required if it's modified.
 */

private object StateSnippet1 {
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

    class HelloActivity : AppCompatActivity() {
        val helloViewModel by viewModels<HelloViewModel>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // binding represents the activity layout, inflated with ViewBinding
            binding.textInput.doAfterTextChanged {
                helloViewModel.onNameChanged(it.toString())
            }

            helloViewModel.name.observe(this) { name ->
                binding.helloText.text = "Hello, $name"
            }
        }
    }
}

private object StateSnippet2 {
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

@Composable private fun StateSnippets3And4() {
    val name: String by helloViewModel.name.observeAsState("")
    val nameState: State<String> = helloViewModel.name.observeAsState("")
}

private object StateSnippet5 {
    @Composable
    fun HelloScreen(helloViewModel: HelloViewModel = viewModel()) {
        // helloViewModel follows the Lifecycle as the Activity or Fragment that calls this
        // composable function. This lifecycle can be modified by callers of HelloScreen.

        // name is the _current_ value of [helloViewModel.name]
        val name: String by helloViewModel.name.observeAsState("")

        HelloInput(name = name, onNameChange = { helloViewModel.onNameChanged(it) })
    }

    @Composable
    fun HelloInput(
        name: String, /* state */
        onNameChange: (String) -> Unit /* event */
    ) {
        Column {
            Text(name)
            TextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") }
            )
        }
    }
}

private object StateSnippet6 {
    @Composable
    fun FancyText(text: String) {
        // by passing text as a parameter to remember, it will re-run the calculation on
        // recomposition if text has changed since the last recomposition
        val formattedText = remember(text) { computeTextFormatting(text) }
        /*...*/
    }
}

private object StateSnippet7 {
    @Composable
    fun ExpandingCard(title: String, body: String) {
        // expanded is "internal state" for ExpandingCard
        var expanded by remember { mutableStateOf(false) }

        // describe the card for the current state of expanded
        Card {
            Column(
                Modifier
                    .width(280.dp)
                    .animateContentSize() // automatically animate size when it changes
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(text = title)

                // content of the card depends on the current value of expanded
                if (expanded) {
                    // TODO: show body & collapse icon
                } else {
                    // TODO: show expand icon
                }
            }
        }
    }
}

@Composable private fun StateSnippets8and9() {
    var expanded: Boolean by remember { mutableStateOf(false) }

    val expandedState: MutableState<Boolean> = remember { mutableStateOf(false) }
}

private object StateSnippet10 {
    /* Part of the API. Look for changes below.
    interface MutableState<T> : State<T> {
        override var value: T
    }
     */
    interface DummyState<T> : State<T> {
        override var value: T
    }
    interface DummMutableState<T> : MutableState<String>
}

private object StateSnippet11 {
    @Composable
    fun ExpandingCard(title: String, body: String) {
        var expanded by remember { mutableStateOf(false) }

        // describe the card for the current state of expanded
        Card {
            Column(
                Modifier
                    .width(280.dp)
                    .animateContentSize() // automatically animate size when it changes
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(text = title)

                // content of the card depends on the current value of expanded
                if (expanded) {
                    Text(text = body, Modifier.padding(top = 8.dp))
                    // change expanded in response to click events
                    IconButton(onClick = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ExpandLess)
                    }
                } else {
                    // change expanded in response to click events
                    IconButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ExpandMore)
                    }
                }
            }
        }
    }
}

private object StateSnippet12 {
    // this stateful composable is only responsible for holding internal state
// and defers the UI to the stateless composable
    @Composable
    fun ExpandingCard(title: String, body: String) {
        var expanded by remember { mutableStateOf(false) }
        ExpandingCard(
            title = title,
            body = body,
            expanded = expanded,
            onExpand = { expanded = true },
            onCollapse = { expanded = false }
        )
    }

    // this stateless composable is responsible for describing the UI based on the state
// passed to it and firing events in response to the buttons being pressed
    @Composable
    fun ExpandingCard(
        title: String,
        body: String,
        expanded: Boolean,
        onExpand: () -> Unit,
        onCollapse: () -> Unit
    ) {
        Card {
            Column(
                Modifier
                    .width(280.dp)
                    .animateContentSize() // automatically animate size when it changes
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(title)
                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    Text(body)
                    IconButton(onClick = onCollapse, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ExpandLess)
                    }
                } else {
                    IconButton(onClick = onExpand, Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.ExpandMore)
                    }
                }
            }
        }
    }
}

private object StateSnippet13 {
    @Composable
    fun ExpandingCard(title: String, body: String) {
        var expanded by savedInstanceState { false }
        ExpandingCard(
            title = title,
            body = body,
            expanded = expanded,
            onExpand = { expanded = true },
            onCollapse = { expanded = false }
        )
    }
}

/*
Fakes needed for snippets to build:
 */

private object binding {
    object helloText {
        var text = ""
    }

    object textInput {
        fun doAfterTextChanged(function: () -> Unit) { }
    }
}

private const val it = 1
private lateinit var helloViewModel: StateSnippet2.HelloViewModel
private fun computeTextFormatting(st: String) {}

private fun ExpandingCard(
    title: String,
    body: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit
) { }

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
