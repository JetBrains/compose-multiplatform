/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

@Sampled
@Composable
fun HorizontalScrollSample() {
    val scrollState = rememberScrollState()
    val gradient = Brush.horizontalGradient(
        listOf(Color.Red, Color.Blue, Color.Green), 0.0f, 10000.0f, TileMode.Repeated
    )
    Box(
        Modifier
            .horizontalScroll(scrollState)
            .size(width = 10000.dp, height = 200.dp)
            .background(brush = gradient)
    )
}

@Sampled
@Composable
fun VerticalScrollExample() {
    val scrollState = rememberScrollState()
    val gradient = Brush.verticalGradient(
        listOf(Color.Red, Color.Blue, Color.Green), 0.0f, 10000.0f, TileMode.Repeated
    )
    Box(
        Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .requiredHeight(10000.dp)
            .background(brush = gradient)
    )
}

@Sampled
@Composable
fun ControlledScrollableRowSample() {
    // Create ScrollState to own it and be able to control scroll behaviour of scrollable Row below
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    Column {
        Row(Modifier.horizontalScroll(scrollState)) {
            repeat(1000) { index ->
                Square(index)
            }
        }
        // Controls for scrolling
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scroll")
            Button(
                onClick = {
                    scope.launch { scrollState.scrollTo(scrollState.value - 1000) }
                }
            ) {
                Text("< -")
            }
            Button(
                onClick = {
                    scope.launch { scrollState.scrollBy(10000f) }
                }
            ) {
                Text("--- >")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Smooth Scroll")
            Button(
                onClick = {
                    scope.launch { scrollState.animateScrollTo(scrollState.value - 1000) }
                }
            ) {
                Text("< -")
            }
            Button(
                onClick = {
                    scope.launch { scrollState.animateScrollBy(10000f) }
                }
            ) {
                Text("--- >")
            }
        }
    }
}

@Composable
private fun Square(index: Int) {
    Box(
        Modifier.size(75.dp, 200.dp).background(colors[index % colors.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(5.dp)
            .size(120.dp, 60.dp)
            .clickable(onClick = onClick)
            .background(color = Color.LightGray),
        contentAlignment = Alignment.Center
    ) { content() }
}
