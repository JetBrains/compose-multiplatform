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
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.HorizontalGradient
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.VerticalGradient
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

private val phrases = listOf(
    "Easy As Pie",
    "Wouldn't Harm a Fly",
    "No-Brainer",
    "Keep On Truckin'",
    "An Arm and a Leg",
    "Down To Earth",
    "Under the Weather",
    "Up In Arms",
    "Cup Of Joe",
    "Not the Sharpest Tool in the Shed",
    "Ring Any Bells?",
    "Son of a Gun",
    "Hard Pill to Swallow",
    "Close But No Cigar",
    "Beating a Dead Horse",
    "If You Can't Stand the Heat, Get Out of the Kitchen",
    "Cut To The Chase",
    "Heads Up",
    "Goody Two-Shoes",
    "Fish Out Of Water",
    "Cry Over Spilt Milk",
    "Elephant in the Room",
    "There's No I in Team",
    "Poke Fun At",
    "Talk the Talk",
    "Know the Ropes",
    "Fool's Gold",
    "It's Not Brain Surgery",
    "Fight Fire With Fire",
    "Go For Broke"
)

@Sampled
@Composable
fun HorizontalScrollSample() {
    val scrollState = rememberScrollState()
    val gradient = HorizontalGradient(
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
    val gradient = VerticalGradient(
        listOf(Color.Red, Color.Blue, Color.Green), 0.0f, 10000.0f, TileMode.Repeated
    )
    Box(
        Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .height(10000.dp)
            .background(brush = gradient)
    )
}

@Sampled
@Composable
fun ScrollableColumnSample() {
    ScrollableColumn {
        phrases.forEach { phrase ->
            Text(phrase, fontSize = 30.sp)
        }
    }
}

@Sampled
@Composable
fun ScrollableRowSample() {
    ScrollableRow {
        repeat(100) { index ->
            Square(index)
        }
    }
}

@Sampled
@Composable
fun ControlledScrollableRowSample() {
    // Create ScrollState to own it and be able to control scroll behaviour of ScrollableRow below
    val scrollState = rememberScrollState()
    Column {
        ScrollableRow(scrollState = scrollState) {
            repeat(1000) { index ->
                Square(index)
            }
        }
        // Controls for scrolling
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scroll")
            Button(onClick = { scrollState.scrollTo(scrollState.value - 1000) }) {
                Text("< -")
            }
            Button(onClick = { scrollState.scrollBy(10000f) }) {
                Text("--- >")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Smooth Scroll")
            Button(onClick = { scrollState.smoothScrollTo(scrollState.value - 1000) }) {
                Text("< -")
            }
            Button(onClick = { scrollState.smoothScrollBy(10000f) }) {
                Text("--- >")
            }
        }
    }
}

@Composable
private fun Square(index: Int) {
    Box(
        Modifier.preferredSize(75.dp, 200.dp).background(colors[index % colors.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.padding(5.dp)
            .preferredSize(120.dp, 60.dp)
            .clickable(onClick = onClick)
            .background(color = Color.LightGray),
        contentAlignment = Alignment.Center
    ) { content() }
}