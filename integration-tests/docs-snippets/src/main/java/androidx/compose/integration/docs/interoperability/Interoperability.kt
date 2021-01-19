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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.interoperability

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.integration.docs.databinding.ExampleLayoutBinding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.listSaver
import androidx.compose.runtime.savedinstancestate.mapSaver
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/interop
 *
 * No action required if it's modified.
 */

private object InteropSnippet1 {
    class ExampleActivity : AppCompatActivity() {
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

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            // Inflate the layout for this fragment
            return inflater.inflate(
                R.layout.fragment_example, container, false
            ).apply {
                findViewById<ComposeView>(R.id.compose_view).setContent {
                    // In Compose world
                    MaterialTheme {
                        Text("Hello Compose!")
                    }
                }
            }
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

        val context = AmbientContext.current
        val customView = remember {
            // Creates custom view
            CustomView(context).apply {
                // Sets up listeners for View -> Compose communication
                myView.setOnClickListener {
                    selectedItem.value = 1
                }
            }
        }

        // Adds view to Compose
        AndroidView({ customView }) { view ->
            // View's been inflated - add logic here if necessary

            // As selectedItem is read here, AndroidView will recompose
            // whenever the state changes
            // Example of Compose -> View communication
            view.coordinator.selectedItem = selectedItem.value
        }
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

@Composable private fun RowScope.InteropSnippet7() {
    Text(
        text = stringResource(R.string.ok),
        modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
    )

    Icon(
        imageVector = vectorResource(R.drawable.ic_plane),
        contentDescription = stringResource(R.string.plane_description),
        tint = colorResource(R.color.Blue700)
    )
}

private object InteropSnippet8 {
    @Composable
    fun rememberCustomView(): CustomView {
        val context = AmbientContext.current
        return remember { CustomView(context).apply { /*...*/ } }
    }
}

/* ktlint-disable indent */
private object InteropSnippet9 {
    class ExampleActivity : AppCompatActivity() {
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
/* ktlint-enable indent */
private object InteropSnippet10 {
    class ExampleViewModel() : ViewModel() { /*...*/ }

    @Composable
    fun MyExample() {
        val viewModel: ExampleViewModel = viewModel()

        // use viewModel here
    }
}

private object InteropSnippet11 {
    @Composable
    fun MyExample() {
        // Returns the same instance as long as the activity is alive,
        // just as if you grabbed the instance from an Activity or Fragment
        val viewModel: ExampleViewModel = viewModel()
    }

    @Composable
    fun MyExample2() {
        val viewModel: ExampleViewModel = viewModel() // Same instance as in MyExample
    }
}

private object InteropSnippet12 {
    @Composable
    fun MyExample() {
        val viewModel: ExampleViewModel = viewModel()
        val dataExample = viewModel.exampleLiveData.observeAsState()

        // Because the state is read here,
        // MyExample recomposes whenever dataExample changes.
        dataExample.value?.let {
            ShowData(dataExample)
        }
    }
}

private object InteropSnippet13 {
    @Composable
    fun fetchImage(url: String): ImageBitmap? {
        // Holds our current image, and will be updated by the onCommit lambda below
        var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }

        onCommit(url) {
            // This onCommit lambda will be invoked every time url changes

            val listener = object : ExampleImageLoader.Listener() {
                override fun onSuccess(bitmap: Bitmap) {
                    // When the image successfully loads, update our image state
                    image = bitmap.asImageBitmap()
                }
            }

            // Now execute the image loader
            val imageLoader = ExampleImageLoader.get()
            imageLoader.load(url).into(listener)

            onDispose {
                // If we leave composition, cancel any pending requests
                imageLoader.cancel(listener)
            }
        }

        // Return the state-backed image property. Any callers of this function
        // will be recomposed once the image finishes loading
        return image
    }
}

private object InteropSnippet14 {
    /** Example suspending loadImage function */
    suspend fun loadImage(url: String): ImageBitmap = TODO()

    @Composable
    fun fetchImage(url: String): ImageBitmap? {
        // This holds our current image, and will be updated by the
        // launchInComposition lambda below
        var image by remember(url) { mutableStateOf<ImageBitmap?>(null) }

        // LaunchedEffect will automatically launch a coroutine to execute
        // the given block. If the `url` changes, any previously launched coroutine
        // will be cancelled, and a new coroutine launched.
        LaunchedEffect(url) {
            image = loadImage(url)
        }

        // Return the state-backed image property
        return image
    }
}

private object InteropSnippet15 {
    @Composable
    fun MyExample() {
        var selectedId by savedInstanceState<String?> { null }
        /*...*/
    }
}

private object InteropSnippet16 {
    // API from kotlin-android-extensions
    /*
    @Parcelize
    data class City(name: String, country: String): Parcelable

    @Composable
    fun MyExample() {
        var selectedCity = savedInstanceState { City("Madrid", "Spain") }
    }
     */
}

private object InteropSnippet17 {
    data class City(val name: String, val country: String)

    val CitySaver = run {
        val nameKey = "Name"
        val countryKey = "Country"
        mapSaver(
            save = { mapOf(nameKey to it.name, nameKey to it.country) },
            restore = { City(it[nameKey] as String, it[countryKey] as String) }
        )
    }

    @Composable
    fun MyExample() {
        var selectedCity = savedInstanceState(CitySaver) { City("Madrid", "Spain") }
    }
}

private object InteropSnippet18 {
    data class City(val name: String, val country: String)

    val CitySaver = listSaver<City, Any>(
        save = { listOf(it.name, it.country) },
        restore = { City(it[0] as String, it[1] as String) }
    )

    @Composable
    fun MyExample() {
        var selectedCity = savedInstanceState(CitySaver) { City("Madrid", "Spain") }
        /*...*/
    }
}

private object InteropSnippet19 {
    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                // We use MdcTheme instead of MaterialTheme {}
                MdcTheme {
                    ExampleComposable(/*...*/)
                }
            }
        }
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
class ExampleViewModel() : ViewModel() {
    val exampleLiveData = MutableLiveData<String>(" ")
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
    fun into(listener: ExampleImageLoader.Listener) { }
}
private fun ExampleComposable() { }
@Composable private fun MdcTheme(content: @Composable () -> Unit) { }

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
}
