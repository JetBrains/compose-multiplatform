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
    "ControlFlowWithEmptyBody", "PropertyName", "CanBeParameter", "PackageDirectoryMismatch"
)

package androidx.compose.integration.docs.interoperabilityui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/interop/compose-in-existing-ui
 *
 * No action required if it's modified.
 */

private object InteropUiSnippet1 {
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

private object InteropUiSnippet2 {
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

private object InteropUiSnippet3 {
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

private object InteropUiSnippet4 {
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

private object InteropUiSnippet5 {
    class ExampleActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            WindowCompat.setDecorFitsSystemWindows(window, false)

            setContent {
                MaterialTheme {
                    MyScreen()
                }
            }
        }
    }

    @Composable
    fun MyScreen() {
        Box {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize() // fill the entire window
                    .imePadding() // padding for the bottom for the IME
                    .imeNestedScroll(), // scroll IME at the bottom
                content = { }
            )
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp) // normal 16dp of padding for FABs
                    .navigationBarsPadding() // Move it out from under the nav bar
                    .imePadding(), // padding for when IME appears
                onClick = { }
            ) {
                Icon( /* ... */)
            }
        }
    }
}

@Composable
fun InteropUiSnippet6(showCautionIcon: Boolean) {
    if (showCautionIcon) {
        CautionIcon(/* ... */)
    }
}

@Composable
fun InteropUiSnippet7() {
    var isEnabled by rememberSaveable { mutableStateOf(false) }

    Column {
        ImageWithEnabledOverlay(isEnabled)
        ControlPanelWithToggle(
            isEnabled = isEnabled,
            onEnabledChanged = { isEnabled = it }
        )
    }
}

private object InteropUiSnippet8 {
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

private object InteropUiSnippet9 {
    // import androidx.compose.ui.platform.ComposeView

    class MyComposeAdapter : RecyclerView.Adapter<MyComposeViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): MyComposeViewHolder {
            return MyComposeViewHolder(ComposeView(parent.context))
        }

        override fun onViewRecycled(holder: MyComposeViewHolder) {
            // Dispose the underlying Composition of the ComposeView
            // when RecyclerView has recycled this ViewHolder
            holder.composeView.disposeComposition()
        }

        /* Other methods */

        // NOTE: DO NOT COPY THE METHODS BELOW IN THE CODE SNIPPETS
        override fun onBindViewHolder(holder: MyComposeViewHolder, position: Int) {
            TODO("Not yet implemented")
        }

        override fun getItemCount(): Int {
            TODO("Not yet implemented")
        }
    }

    class MyComposeViewHolder(
        val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        /* ... */
    }
}

private object InteropUiSnippet10 {
    // import androidx.compose.ui.platform.ViewCompositionStrategy

    class MyComposeViewHolder(
        val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {

        init {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }

        fun bind(input: String) {
            composeView.setContent {
                MdcTheme {
                    Text(input)
                }
            }
        }
    }
}

/*
Fakes needed for snippets to build:
 */

private object R {
    object string {
        const val something = 1
    }
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
private fun Icon() {
}

@Composable
private fun CautionIcon() {
}

@Composable
private fun ImageWithEnabledOverlay(isEnabled: Boolean) {
}

@Composable
private fun ControlPanelWithToggle(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit
) {
}

private class WindowCompat {
    companion object {
        fun setDecorFitsSystemWindows(window: Any, bool: Boolean) {}
    }
}

private fun Modifier.navigationBarsPadding(): Modifier = this

private fun Modifier.fillMaxSize(): Modifier = this

private fun Modifier.imePadding(): Modifier = this

private fun Modifier.imeNestedScroll(): Modifier = this

private class ActivityExampleBinding {
    val root: Int = 0
    lateinit var callToAction: InteropUiSnippet1.CallToActionViewButton
    companion object {
        fun inflate(li: LayoutInflater): ActivityExampleBinding { TODO() }
    }
}
