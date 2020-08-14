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

package androidx.compose.foundation.layout.demos

import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.WithConstraints
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun RtlDemo() {
    Column(verticalArrangement = Arrangement.SpaceEvenly) {
        Text("TEXT", Modifier.gravity(Alignment.CenterHorizontally))
        testText()
        Text("ROW", Modifier.gravity(Alignment.CenterHorizontally))
        testRow()
        Text("ROW WITH LTR ROW IN BETWEEN", Modifier.gravity(Alignment.CenterHorizontally))
        testRow_modifier()
        Text("RELATIVE TO SIBLINGS", Modifier.gravity(Alignment.CenterHorizontally))
        testSiblings()
        Text(
            "PLACE WITH AUTO RTL SUPPORT IN CUSTOM LAYOUT",
            Modifier.gravity(Alignment.CenterHorizontally)
        )
        CustomLayout(true)
        Text(
            "PLACE WITHOUT RTL SUPPORT IN CUSTOM LAYOUT",
            Modifier.gravity(Alignment.CenterHorizontally)
        )
        CustomLayout(false)
        Text("WITH CONSTRAINTS", Modifier.gravity(Alignment.CenterHorizontally))
        Providers(LayoutDirectionAmbient provides LayoutDirection.Ltr) {
            LayoutWithConstraints("LD: set LTR via ambient")
        }
        Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
            LayoutWithConstraints("LD: set RTL via ambient")
        }
        LayoutWithConstraints(text = "LD: locale")
        Text("STACK EXAMPLE", Modifier.gravity(Alignment.CenterHorizontally))
        StackExample()
    }
}

@Composable
fun StackExample() {
    Stack(Modifier.fillMaxSize().background(Color.LightGray)) {
        Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
            Stack(boxSize.then(Modifier.background(Color.Red))) {}
        }
        Stack(Modifier.fillMaxSize().wrapContentHeight(Alignment.CenterVertically)) {
            Stack(boxSize.then(Modifier.background(Color.Green))) {}
        }
        Stack(Modifier.fillMaxSize().wrapContentSize(Alignment.BottomEnd)) {
            Stack(boxSize.then(Modifier.background(Color.Blue))) {}
        }
    }
}

private val boxSize = Modifier.preferredSize(50.dp, 20.dp)
private val size = Modifier.preferredSize(10.dp, 10.dp)

@Composable
private fun testRow() {
    Row {
        Stack(boxSize.background(color = Color.Red)) {}
        Stack(boxSize.background(color = Color.Green)) {}
        Row {
            Stack(boxSize.background(color = Color.Magenta)) {}
            Stack(boxSize.background(color = Color.Yellow)) {}
            Stack(boxSize.background(color = Color.Cyan)) {}
        }
        Stack(boxSize.background(color = Color.Blue)) {}
    }
}

@Composable
private fun testRow_modifier() {
    Row {
        Stack(boxSize.background(Color.Red)) {}
        Stack(boxSize.background(Color.Green)) {}
        Providers(LayoutDirectionAmbient provides LayoutDirection.Ltr) {
            Row {
                Stack(boxSize.background(Color.Magenta)) {}
                Stack(boxSize.background(Color.Yellow)) {}
                Stack(boxSize.background(Color.Cyan)) {}
            }
        }
        Stack(boxSize.background(color = Color.Blue)) {}
    }
}

@Composable
private fun testText() {
    Column {
        Text("Text.")
        Text("Text filling max width.", Modifier.fillMaxWidth())
        Text("שלום!")
        Text("שלום!", Modifier.fillMaxWidth())
        Text("-->")
        Text("-->", Modifier.fillMaxWidth())
    }
}

@Composable
private fun testSiblings() {
    Column {
        Stack(boxSize.background(color = Color.Red).alignWithSiblings { p -> p.width }
        ) {}
        Stack(boxSize.background(color = Color.Green).alignWithSiblings { p -> p.width / 2 }
        ) {}
        Stack(boxSize.background(color = Color.Blue).alignWithSiblings { p -> p.width / 4 }
        ) {}
    }
}

@Composable
private fun CustomLayout(rtlSupport: Boolean) {
    Layout(children = @Composable {
        Stack(boxSize.background(color = Color.Red)) {}
        Stack(boxSize.background(color = Color.Green)) {}
        Stack(boxSize.background(color = Color.Blue)) {}
    }) { measurables, constraints ->
        val p = measurables.map { e ->
            e.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }
        val w = p.fold(0) { sum, e -> sum + e.width }
        val h = p.maxByOrNull { it.height }!!.height
        layout(w, h) {
            var xPosition = 0
            for (child in p) {
                child.place(IntOffset(xPosition, 0))
                if (rtlSupport) {
                    child.placeRelative(IntOffset(xPosition, 0))
                } else {
                    child.place(IntOffset(xPosition, 0))
                }
                xPosition += child.width
            }
        }
    }
}

@Composable
private fun LayoutWithConstraints(text: String) {
    WithConstraints {
        val w = maxWidth / 3
        val color = if (LayoutDirectionAmbient.current == LayoutDirection.Ltr) {
            Color.Red
        } else {
            Color.Magenta
        }
        Stack(Modifier.preferredSize(w, 20.dp).background(color)) {
            Text(text, Modifier.gravity(Alignment.Center))
        }
    }
}
