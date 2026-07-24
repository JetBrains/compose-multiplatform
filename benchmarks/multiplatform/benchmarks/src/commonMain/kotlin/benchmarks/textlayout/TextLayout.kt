/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.textlayout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlinx.coroutines.isActive

private const val ITEM_COUNT = 2000

@Composable
fun TextLayout() {
    val listState = rememberLazyListState()
    var frame by remember { mutableStateOf(0) }
    var scrollForward by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { }
            val currentItem = listState.firstVisibleItemIndex
            if (currentItem == 0) scrollForward = true
            if (currentItem > ITEM_COUNT - 100) scrollForward = false
            listState.scrollBy(if (scrollForward) 67f else -67f)
            frame++
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(ITEM_COUNT) { index ->
            TextLayoutItem(index, frame)
        }
    }
}

@Composable
private fun TextLayoutItem(index: Int, frame: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
    ) {
        repeat(12) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(10) { col ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF3A3A3A))
                    ) {
                        Text(
                            "$frame R$row:C$col",
                            color = Color.White,
                            fontSize = 6.sp
                        )
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(Random.nextInt(0xFFFFFF) or 0xFF000000.toInt()))
                            )
                            Column {
                                Text("$frame #$index", color = Color.Gray, fontSize = 6.sp)
                                Text("Item", color = Color.Gray, fontSize = 6.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(10) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF4A4A4A))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
