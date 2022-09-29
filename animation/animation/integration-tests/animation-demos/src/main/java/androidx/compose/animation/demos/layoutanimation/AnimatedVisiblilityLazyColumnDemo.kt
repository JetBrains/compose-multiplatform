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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun AnimatedVisibilityLazyColumnDemo() {
    Column {
        val model = remember { MyModel() }
        Row(Modifier.fillMaxWidth()) {
            Button(
                { model.addNewItem() },
                modifier = Modifier.padding(15.dp).weight(1f)
            ) {
                Text("Add")
            }
        }

        LaunchedEffect(model) {
            snapshotFlow {
                model.items.firstOrNull { it.visible.isIdle && !it.visible.targetState }
            }.collect {
                if (it != null) {
                    model.pruneItems()
                }
            }
        }
        LazyColumn {
            items(model.items, key = { it.itemId }) { item ->
                AnimatedVisibility(
                    item.visible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(Modifier.fillMaxWidth().requiredHeight(90.dp).background(item.color)) {
                        Button(
                            { model.removeItem(item) },
                            modifier = Modifier.align(CenterEnd).padding(15.dp)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        Button(
            { model.removeAll() },
            modifier = Modifier.align(End).padding(15.dp)
        ) {
            Text("Clear All")
        }
    }
}

private class MyModel {
    private val _items: MutableList<ColoredItem> = mutableStateListOf()
    private var lastItemId = 0
    val items: List<ColoredItem> = _items

    class ColoredItem(val visible: MutableTransitionState<Boolean>, val itemId: Int) {
        val color: Color
            get() = turquoiseColors.let {
                it[itemId % it.size]
            }
    }

    fun addNewItem() {
        lastItemId++
        _items.add(
            ColoredItem(
                MutableTransitionState(false).apply { targetState = true },
                lastItemId
            )
        )
    }

    fun removeItem(item: ColoredItem) {
        item.visible.targetState = false
    }

    @OptIn(ExperimentalTransitionApi::class)
    fun pruneItems() {
        _items.removeAll(items.filter { it.visible.isIdle && !it.visible.targetState })
    }

    fun removeAll() {
        _items.forEach {
            it.visible.targetState = false
        }
    }
}

internal val turquoiseColors = listOf(
    Color(0xff07688C),
    Color(0xff1986AF),
    Color(0xff50B6CD),
    Color(0xffBCF8FF),
    Color(0xff8AEAE9),
    Color(0xff46CECA)
)
