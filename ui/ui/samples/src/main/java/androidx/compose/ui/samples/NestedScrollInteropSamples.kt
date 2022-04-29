/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.samples

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ComposeInCooperatingViewNestedScrollInteropSample() {
    val nestedSrollInterop = rememberNestedScrollInteropConnection()
    // Add the nested scroll connection to your top level @Composable element
    // using the nestedScroll modifier.
    LazyColumn(modifier = Modifier.nestedScroll(nestedSrollInterop)) {
        items(20) { item ->
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(item.toString())
            }
        }
    }
}

@Sampled
@Composable
fun ViewInComposeNestedScrollInteropSample() {
    Box(
        Modifier
            .fillMaxSize()
            .scrollable(rememberScrollableState {
                // view world deltas should be reflected in compose world
                // components that participate in nested scrolling
                it
            }, Orientation.Vertical)
    ) {
        AndroidView(
            { context ->
                LayoutInflater.from(context)
                    .inflate(android.R.layout.activity_list_item, null)
                    .apply {
                        // Nested Scroll Interop will be Enabled when
                        // nested scroll is enabled for the root view
                        ViewCompat.setNestedScrollingEnabled(this, true)
                    }
            }
        )
    }
}

private val ToolbarHeight = 48.dp

@Composable
fun CollapsingToolbarComposeViewComposeNestedScrollInteropSample() {
    val toolbarHeightPx = with(LocalDensity.current) { ToolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    // Compose Scrollable
    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        TopAppBar(
            modifier = Modifier
                .height(ToolbarHeight)
                .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
            title = { Text("toolbar offset is ${toolbarOffsetHeightPx.value}") }
        )
        // Android View
        AndroidView(
            factory = { context -> AndroidViewWithCompose(context) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun AndroidViewWithCompose(context: Context): View {
    return LayoutInflater.from(context)
        .inflate(R.layout.three_fold_nested_scroll_interop, null).apply {
            with(findViewById<ComposeView>(R.id.compose_view)) {
                // Compose
                setContent { LazyColumnWithNestedScrollInteropEnabled() }
            }
        }.also {
            ViewCompat.setNestedScrollingEnabled(it, true)
        }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LazyColumnWithNestedScrollInteropEnabled() {
    LazyColumn(
        modifier = Modifier.nestedScroll(
            rememberNestedScrollInteropConnection()
        ),
        contentPadding = PaddingValues(top = ToolbarHeight)
    ) {
        item {
            Text("This is a Lazy Column")
        }
        items(40) { item ->
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(item.toString())
            }
        }
    }
}
