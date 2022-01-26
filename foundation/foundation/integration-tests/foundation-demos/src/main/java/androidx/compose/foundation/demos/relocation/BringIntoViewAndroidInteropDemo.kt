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

package androidx.compose.foundation.demos.relocation

import android.content.Context
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

// TODO(b/216652644) This demo is currently broken.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BringIntoViewAndroidInteropDemo() {
    Column {
        Text(
            "This is a Compose vertical scrollable, with an Android scrollable inside it, with a " +
                "Compose BringIntoViewRequester inside of that."
        )

        val topStartRequester = remember { BringIntoViewRequester() }
        val bottomEndRequester = remember { BringIntoViewRequester() }
        val scope = rememberCoroutineScope()

        Box(
            Modifier
                .border(2.dp, Color.Blue)
                .size(200.dp, 200.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AndroidView(::AndroidScrollable) {
                it.setContent {
                    Box(Modifier.size(500.dp)) {
                        Text(
                            "Top-start",
                            Modifier
                                .align(Alignment.TopStart)
                                .bringIntoViewRequester(topStartRequester)
                        )
                        Text(
                            "Bottom-end",
                            Modifier
                                .align(Alignment.BottomEnd)
                                .bringIntoViewRequester(bottomEndRequester)
                        )
                    }
                }
            }
        }

        Button(onClick = {
            scope.launch {
                topStartRequester.bringIntoView()
            }
        }) {
            Text("Bring top-start into view")
        }
        Button(onClick = {
            scope.launch {
                bottomEndRequester.bringIntoView()
            }
        }) {
            Text("Bring bottom-end into view")
        }
    }
}

private class AndroidScrollable(context: Context) : HorizontalScrollView(context) {
    private val composeView = ComposeView(context)

    init {
        addView(composeView, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    }

    fun setContent(content: @Composable () -> Unit) {
        composeView.setContent(content)
    }
}