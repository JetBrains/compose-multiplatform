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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedVisibilityLazyColumnDemo() {
    var itemNum by remember { mutableStateOf(0) }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Button(
                { itemNum = itemNum + 1 },
                enabled = itemNum <= turquoiseColors.size - 1,
                modifier = Modifier.padding(15.dp).weight(1f)
            ) {
                Text("Add")
            }

            Button(
                { itemNum = itemNum - 1 },
                enabled = itemNum >= 1,
                modifier = Modifier.padding(15.dp).weight(1f)
            ) {
                Text("Remove")
            }
        }
        LazyColumnForIndexed(turquoiseColors) { i, color ->
            AnimatedVisibility(
                (turquoiseColors.size - itemNum) <= i,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Spacer(Modifier.fillMaxWidth().height(90.dp).background(color))
            }
        }

        Button(
            { itemNum = 0 },
            modifier = Modifier.align(End).padding(15.dp)
        ) {
            Text("Clear All")
        }
    }
}

private val turquoiseColors = listOf(
    Color(0xff07688C),
    Color(0xff1986AF),
    Color(0xff50B6CD),
    Color(0xffBCF8FF),
    Color(0xff8AEAE9),
    Color(0xff46CECA)
)
