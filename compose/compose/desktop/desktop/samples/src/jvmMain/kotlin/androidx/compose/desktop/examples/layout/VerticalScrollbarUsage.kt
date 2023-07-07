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

package androidx.compose.desktop.examples.layout

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun VerticalScrollbarUsage() = Box {
    val state = rememberLazyListState()
    val itemCount = 100000
    val heights = remember {
        val random = Random(24)
        (0 until itemCount).map { random.nextFloat() }
    }

    LazyColumn(Modifier.graphicsLayer(alpha = 0.5f), state = state) {
        items((0 until itemCount).toList()) { i ->
            val itemHeight = 20.dp + 20.dp * heights[i]
            Text(i.toString(), Modifier.graphicsLayer(alpha = 0.5f).height(itemHeight))
        }
    }

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd)
    )
}
