/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Preview
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyColumnDragAndDropDemo() {
    var list by remember { mutableStateOf(List(50) { it }) }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        list = list.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    LazyColumn(
        modifier = Modifier.dragContainer(dragDropState),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(list, key = { _, item -> item }) { index, item ->
            DraggableItem(dragDropState, index) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp)
                Card(elevation = elevation) {
                    Text("Item $item", Modifier.fillMaxWidth().padding(20.dp))
                }
            }
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            onMove = onMove,
            scope = scope
        )
    }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

class DragDropState internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableStateOf(0f)
    private var draggingItemInitialOffset by mutableStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != null) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 1f
                    )
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = null
        draggingItemInitialOffset = 0
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..item.offsetEnd &&
                draggingItem.index != item.index
        }
        if (targetItem != null) {
            val scrollToIndex = if (targetItem.index == state.firstVisibleItemIndex) {
                draggingItem.index
            } else if (draggingItem.index == state.firstVisibleItemIndex) {
                targetItem.index
            } else {
                null
            }
            if (scrollToIndex != null) {
                scope.launch {
                    // this is needed to neutralize automatic keeping the first item first.
                    state.scrollToItem(scrollToIndex, state.firstVisibleItemScrollOffset)
                    onMove.invoke(draggingItem.index, targetItem.index)
                }
            } else {
                onMove.invoke(draggingItem.index, targetItem.index)
            }
            draggingItemIndex = targetItem.index
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 ->
                    (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier {
    return pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset = offset)
            },
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.draggingItemIndex
    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.draggingItemOffset
            }
    } else if (index == dragDropState.previousIndexOfDraggedItem) {
        Modifier.zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.previousItemOffset.value
            }
    } else {
        Modifier.animateItemPlacement()
    }
    Column(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}
