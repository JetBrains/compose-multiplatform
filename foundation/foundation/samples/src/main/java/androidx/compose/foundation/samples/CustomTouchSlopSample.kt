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

package androidx.compose.foundation.samples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun CustomTouchSlopSample() {
    val originalTouchSlop = LocalViewConfiguration.current.touchSlop

    CustomTouchSlopProvider(newTouchSlop = originalTouchSlop * 3) {
        LazyColumn {
            items(100) {
                Spacer(Modifier.padding(10.dp))
                LongListOfItems(originalTouchSlop)
            }
        }
    }
}

@Composable
fun CustomTouchSlopProvider(
    newTouchSlop: Float,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalViewConfiguration provides CustomTouchSlopAngle(
            newTouchSlop,
            LocalViewConfiguration.current
        )
    ) {
        content()
    }
}

class CustomTouchSlopAngle(
    private val customTouchSlop: Float,
    currentConfiguration: ViewConfiguration
) : ViewConfiguration by currentConfiguration {
    override val touchSlop: Float
        get() = customTouchSlop
}

@Composable
fun LongListOfItems(originalTouchSlop: Float) {
    CustomTouchSlopProvider(newTouchSlop = originalTouchSlop / 3) {
        LazyRow {
            items(100) {
                Box(modifier = Modifier.size(80.dp).padding(4.dp).background(Color.Gray)) {
                    Text(text = it.toString(), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}