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

package androidx.compose.runtime.benchmark.deeptree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val blueBackground = Modifier.background(color = Color.Blue)
val magentaBackground = Modifier.background(color = Color.Magenta)
val blackBackground = Modifier.background(color = Color.Black)

@Composable
fun Terminal(style: Int) {
    val background = when (style) {
        0 -> blueBackground
        1 -> blackBackground
        else -> magentaBackground
    }
    Box(modifier = Modifier.fillMaxSize().then(background))
}

@Composable
fun Stack(vertical: Boolean, content: @Composable () -> Unit) {
    if (vertical) {
        Column(Modifier.fillMaxHeight()) { content() }
    } else {
        Row(Modifier.fillMaxWidth()) { content() }
    }
}

/**
 *
 * This Component will emit `breadth ^ depth` Terminal components.
 *
 *
 * @param depth - increasing this will determine how many nested <Stack> elements will result in the tree. Higher
 * numbers would be a proxy for very complicated layouts
 *
 * @param breadth - increasing this will increase the number of nodes at each level. Correlates to exponential
 * growth in the number of nodes in the tree, so be careful making it too large.
 *
 * @param wrap - to make the depth of the composition tree greater, we can increase this and it will just wrap
 * the component this many times at each level. It will not increase the number of layout nodes in the tree, but
 * will make composition more expensive.
 *
 * @param id - an int that determines the style of the next terminal
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DeepTree(depth: Int, breadth: Int, wrap: Int, id: Int = 0) {
//    if (wrap > 0) {
//        Container {
//            DeepTree(depth=depth, breadth=breadth, wrap=wrap - 1, id=id)
//        }
//    } else {
    Stack(vertical = depth % 2 == 0) {
        if (depth == 0) {
            Terminal(style = id % 3)
        } else {
            repeat(breadth) {
                Box(Modifier.fillMaxSize().then(blueBackground))
            }
        }
    }
//    }
}
