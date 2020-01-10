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

package androidx.compose.benchmark.deeptree

import androidx.compose.Composable
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.FlexScope
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row

@Composable
fun Terminal(style: Int) {
    val color = when (style) {
        0 -> Color.Blue
        1 -> Color.Black
        else -> Color.Magenta
    }
    ColoredRect(color = color, height = 16.dp, width = 16.dp)
}

@Composable
fun Stack(vertical: Boolean, children: @Composable() FlexScope.() -> Unit) {
    if (vertical) {
        Column(LayoutHeight.Fill, children = children)
    } else {
        Row(LayoutWidth.Fill, children = children)
    }
}

@Composable
fun Container(children: @Composable() () -> Unit) {
    // non-layout node component. just adds depth to the composition hierarchy.
    children()
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
                    ColoredRect(color = Color.Blue, height = 16.dp, width = 16.dp)
//                    DeepTree(depth=depth - 1, wrap=wrap, breadth=breadth, id=id)
                }
            }
        }
//    }
}