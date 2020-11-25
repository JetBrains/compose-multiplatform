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

@file:Suppress("PLUGIN_WARNING")

package androidx.compose.ui.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints

@Composable
fun HeaderFooterLayout(
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Layout({
        Box(Modifier.layoutId("header")) { header() }
        Box(Modifier.layoutId("footer")) { footer() }
        content()
    }) { measurables, constraints ->
        val headerPlaceable = measurables.first { it.layoutId == "header" }.measure(
            Constraints.fixed(constraints.maxWidth, 100)
        )
        val footerPadding = 50
        val footerPlaceable = measurables.first { it.layoutId == "footer" }.measure(
            Constraints.fixed(constraints.maxWidth - footerPadding * 2, 100)
        )

        val contentMeasurables = measurables.filter { it.layoutId == null }
        val itemHeight =
            (constraints.maxHeight - headerPlaceable.height - footerPlaceable.height) /
                contentMeasurables.size
        val contentPlaceables = contentMeasurables.map { measurable ->
            measurable.measure(Constraints.fixed(constraints.maxWidth, itemHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.placeRelative(0, 0)
            footerPlaceable.placeRelative(
                footerPadding,
                constraints.maxHeight - footerPlaceable.height
            )
            var top = headerPlaceable.height
            contentPlaceables.forEach { placeable ->
                placeable.placeRelative(0, top)
                top += itemHeight
            }
        }
    }
}

@Composable
fun MultipleCollectTest() {
    val header = @Composable {
        Box(Modifier.fillMaxSize().background(Color(android.graphics.Color.GRAY)))
    }
    val footer = @Composable {
        Box(Modifier.fillMaxSize().background(Color(android.graphics.Color.BLUE)))
    }
    HeaderFooterLayout(header = header, footer = footer) {
        Box(Modifier.fillMaxSize().background(Color(android.graphics.Color.GREEN)))
        Box(Modifier.fillMaxSize().background(Color(android.graphics.Color.YELLOW)))
    }
}
