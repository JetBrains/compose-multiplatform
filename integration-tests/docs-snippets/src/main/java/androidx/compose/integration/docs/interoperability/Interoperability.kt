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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.integration.docs.databinding.ExampleLayoutBinding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
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

private object InteropSnippet9 {
    // import com.google.android.material.composethemeadapter.MdcTheme

    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                // Use MdcTheme instead of MaterialTheme
                // Colors, typography, and shape have been read from the
                // View-based theme used in this Activity
                MdcTheme {
                    ExampleComposable(/*...*/)
                }
            }
        }
    }
}

private object InteropSnippet10 {
    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                AppCompatTheme {
                    // Colors, typography, and shape have been read from the
                    // View-based theme used in this Activity
                    ExampleComposable(/*...*/)
                }
            }
        }
    }
}

private object InteropSnippet11 {
    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            WindowCompat.setDecorFitsSystemWindows(window, false)

            setContent {
                MaterialTheme {
                    ProvideWindowInsets {
                        MyScreen()
                    }
                }
            }
        }
    }

    @Composable
    fun MyScreen() {
        Box {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp) // normal 16dp of padding for FABs
                    .navigationBarsPadding(), // Move it out from under the nav bar
                onClick = { }
            ) {
                Icon( /* ... */)
            }
        }
    }
}

private object InteropSnippet12 {
    @Composable
    fun MyComposable() {
        BoxWithConstraints {
            if (minWidth < 480.dp) {
                /* Show grid with 4 columns */
            } else if (minWidth < 720.dp) {
                /* Show grid with 8 columns */
            } else {
                /* Show grid with 12 columns */
            }
        }
    }
}

private object InteropSnippet13 {
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
}

private object InteropSnippet14 {
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

private object InteropSnippet15 {
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

private object InteropSnippet16 {
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

private object InteropSnippet17 {
    @Composable
    fun CallToActionButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary
            ),
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(text)
        }
    }

    class CallToActionViewButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : AbstractComposeView(context, attrs, defStyle) {

        var text by mutableStateOf<String>("")
        var onClick by mutableStateOf<() -> Unit>({})

        @Composable
        override fun Content() {
            YourAppTheme {
                CallToActionButton(text, onClick)
            }
        }
    }
}

private object InteropSnippet18 {
    class ExampleActivity : Activity() {

        private lateinit var binding: ActivityExampleBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityExampleBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.callToAction.apply {
                text = getString(R.string.something)
                onClick = { /* Do something */ }
            }
        }
    }
}

private object InteropSnippet19 {
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
                    onSystemEvent(intent)
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
        const val something = 6
    }

    object dimen {
        const val padding_small = 7
    }

    object drawable {
        const val ic_plane = 8
    }

    object color {
        const val Blue700 = 9
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
class ExampleViewModel : ViewModel() {
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

private fun ExampleComposable() {}
@Composable
private fun MdcTheme(content: @Composable () -> Unit) {
}

@Composable
private fun AppCompatTheme(content: @Composable () -> Unit) {
}

@Composable
private fun BlueTheme(content: @Composable () -> Unit) {
}

@Composable
private fun PinkTheme(content: @Composable () -> Unit) {
}

@Composable
private fun YourAppTheme(content: @Composable () -> Unit) {
}

@Composable
private fun ProvideWindowInsets(content: @Composable () -> Unit) {
}

@Composable
private fun Icon() {
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
}

private class WindowCompat {
    companion object {
        fun setDecorFitsSystemWindows(window: Any, bool: Boolean) {}
    }
}

private fun Modifier.navigationBarsPadding(): Modifier = this

private class GreetingViewModel : ViewModel() {
    val _message = MutableLiveData("")
    val message: LiveData<String> = _message
}
private class GreetingViewModelFactory(val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        TODO("Not yet implemented")
    }
}
private class ActivityExampleBinding {
    val root: Int = 0
    lateinit var callToAction: InteropSnippet17.CallToActionViewButton
    companion object {
        fun inflate(li: LayoutInflater): ActivityExampleBinding { TODO() }
    }
}