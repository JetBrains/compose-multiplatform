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

package androidx.compose.ui.demos.viewinterop

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.ui.demos.R
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

private val ToolbarHeight = 48.dp

@SuppressLint("UnnecessaryLambdaCreation")
@Composable
private fun OuterComposeWithNestedScroll(factory: (Context) -> View) {
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
            factory = { context -> factory(context) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun AndroidViewWithNestedScrollEnabled(context: Context): View {
    return LayoutInflater.from(context)
        .inflate(R.layout.android_in_compose_nested_scroll_interop, null).apply {
            with(findViewById<RecyclerView>(R.id.main_list)) {
                layoutManager =
                    LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = NestedScrollInteropAdapter()
            }
        }.also {
            ViewCompat.setNestedScrollingEnabled(it, true)
        }
}

@Composable
internal fun NestedScrollInteropComposeParentWithAndroidChild() {
    OuterComposeWithNestedScroll { context ->
        AndroidViewWithNestedScrollEnabled(context)
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

@Composable
internal fun ComposeViewComposeNestedInterop() {
    OuterComposeWithNestedScroll { context ->
        LayoutInflater.from(context)
            .inflate(R.layout.three_fold_nested_scroll_interop, null).apply {
                with(findViewById<ComposeView>(R.id.compose_view)) {
                    setContent { LazyColumnWithNestedScrollInteropEnabled() }
                }
            }.also {
                ViewCompat.setNestedScrollingEnabled(it, true)
            }
    }
}

private class NestedScrollInteropAdapter :
    RecyclerView.Adapter<NestedScrollInteropAdapter.NestedScrollInteropViewHolder>() {
    val items = (1..100).map { it.toString() }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NestedScrollInteropViewHolder {
        return NestedScrollInteropViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.android_in_compose_nested_scroll_interop_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: NestedScrollInteropViewHolder, position: Int) {
        if (position == 0) {
            holder.bind("This is a RV")
        } else {
            holder.bind(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    class NestedScrollInteropViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: String) {
            itemView.findViewById<TextView>(R.id.list_item).text = item
        }
    }
}

@ExperimentalComposeUiApi
internal class ComposeInAndroidCoordinatorLayout : ComponentActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_coordinator_layout)
        findViewById<ComposeView>(R.id.compose_view).apply {
            setContent { LazyColumnWithNestedScrollInteropEnabled() }
        }
    }
}

@ExperimentalComposeUiApi
internal class ViewComposeViewNestedScrollInteropDemo : ComponentActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_in_android_coordinator_layout)
        findViewById<ComposeView>(R.id.compose_view).apply {
            setContent {
                val nestedScrollInterop = rememberNestedScrollInteropConnection()
                Box(modifier = Modifier.nestedScroll(nestedScrollInterop)) {
                    AndroidView({ context ->
                        AndroidViewWithNestedScrollEnabled(context)
                    }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}