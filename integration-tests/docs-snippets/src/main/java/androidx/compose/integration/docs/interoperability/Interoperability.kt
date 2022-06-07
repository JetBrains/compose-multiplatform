// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
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
@file:Suppress(
    "unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER",
    "RedundantSuspendModifier", "CascadeIf", "ClassName", "RemoveExplicitTypeArguments",
    "ControlFlowWithEmptyBody", "PropertyName", "CanBeParameter"
)

package androidx.compose.integration.docs.interoperability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.integration.docs.databinding.ExampleLayoutBinding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/interop/interop-apis
 *
 * No action required if it's modified.
 */

private object InteropSnippet1 {
    class ExampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent { // In here, we can call composables!
                MaterialTheme {
                    Greeting(name = "compose")
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }
}

private object InteropSnippet2 {
    class ExampleFragment : Fragment() {

        private var _binding: FragmentExampleBinding? = null
        // This property is only valid between onCreateView and onDestroyView.
        private val binding get() = _binding!!

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentExampleBinding.inflate(inflater, container, false)
            val view = binding.root
            binding.composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    // In Compose world
                    MaterialTheme {
                        Text("Hello Compose!")
                    }
                }
            }
            return view
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }
}

private object InteropSnippet3 {
    class ExampleFragment : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return ComposeView(requireContext()).apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme {
                        // In Compose world
                        Text("Hello Compose!")
                    }
                }
            }
        }
    }
}

/* ktlint-disable indent */
private object InteropSnippet4 {
    // TW: Do not use this snippet.
    // This snippet simplifies too much code from Fragment and View so check out the following
    // snippet for changes:

    class ExampleFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return LinearLayout(context).apply {
                addView(ComposeView(context).apply {
                    id = R.id.compose_view_x
                })
            }
        }
    }
}
/* ktlint-enable indent */

private object InteropSnippet5 {
    @Composable
    fun CustomView() {
        val selectedItem = remember { mutableStateOf(0) }

        // Adds view to Compose
        AndroidView(
            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = { context ->
                // Creates custom view
                CustomView(context).apply {
                    // Sets up listeners for View -> Compose communication
                    myView.setOnClickListener {
                        selectedItem.value = 1
                    }
                }
            },
            update = { view ->
                // View's been inflated or state read in this block has been updated
                // Add logic here if necessary

                // As selectedItem is read here, AndroidView will recompose
                // whenever the state changes
                // Example of Compose -> View communication
                view.coordinator.selectedItem = selectedItem.value
            }
        )
    }

    @Composable
    fun ContentExample() {
        Column(Modifier.fillMaxSize()) {
            Text("Look at this CustomView!")
            CustomView()
        }
    }
}

private object InteropSnippet6 {
    @Composable
    fun AndroidViewBindingExample() {
        AndroidViewBinding(ExampleLayoutBinding::inflate) {
            exampleView.setBackgroundColor(Color.GRAY)
        }
    }
}

private object InteropSnippet7 {
    @Composable
    fun rememberCustomView(): CustomView {
        val context = LocalContext.current
        return remember { CustomView(context).apply { /*...*/ } }
    }
}

/* ktlint-disable indent */
private object InteropSnippet8 {
    class ExampleActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // get data from savedInstanceState
            setContent {
                MaterialTheme {
                    ExampleComposable(data, onButtonClick = {
                        startActivity(/*...*/)
                    })
                }
            }
        }
    }

    @Composable
    fun ExampleComposable(data: DataExample, onButtonClick: () -> Unit) {
        Button(onClick = onButtonClick) {
            Text(data.title)
        }
    }
}

private object InteropSnippet9 {
    @Composable
    fun SystemBroadcastReceiver(
        systemAction: String,
        onSystemEvent: (intent: Intent?) -> Unit
    ) {
        // Grab the current context in this part of the UI tree
        val context = LocalContext.current

        // Safely use the latest onSystemEvent lambda passed to the function
        val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)

        // If either context or systemAction changes, unregister and register again
        DisposableEffect(context, systemAction) {
            val intentFilter = IntentFilter(systemAction)
            val broadcast = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    currentOnSystemEvent(intent)
                }
            }

            context.registerReceiver(broadcast, intentFilter)

            // When the effect leaves the Composition, remove the callback
            onDispose {
                context.unregisterReceiver(broadcast)
            }
        }
    }

    @Composable
    fun HomeScreen() {

        SystemBroadcastReceiver(Intent.ACTION_BATTERY_CHANGED) { batteryStatus ->
            val isCharging = /* Get from batteryStatus ... */ true
            /* Do something if the device is charging */
        }

        /* Rest of the HomeScreen */
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object layout {
        const val fragment_example = 1
    }

    object id {
        const val compose_view = 2
        const val compose_view_x = 3
    }

    object string {
        const val ok = 4
        const val plane_description = 5
    }

    object dimen {
        const val padding_small = 6
    }

    object drawable {
        const val ic_plane = 7
    }

    object color {
        const val Blue700 = 8
    }
}

private class CustomView(context: Context) : View(context) {
    class Coord(var selectedItem: Int = 0)

    val coordinator = Coord()
    lateinit var myView: View
}

private class DataExample(val title: String = "")

private val data = DataExample()
private fun startActivity(): Nothing = TODO()
private class ExampleViewModel : ViewModel() {
    val exampleLiveData = MutableLiveData(" ")
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

private open class Fragment {

    lateinit var context: Context
    open fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        TODO("not implemented")
    }

    fun requireContext(): Context = TODO()

    open fun onDestroyView() { }
}

private class FragmentExampleBinding {
    val root: View = TODO()
    var composeView: ComposeView
    companion object {
        fun inflate(
            li: LayoutInflater,
            container: ViewGroup?,
            boolean: Boolean
        ): FragmentExampleBinding { TODO() }
    }
}
