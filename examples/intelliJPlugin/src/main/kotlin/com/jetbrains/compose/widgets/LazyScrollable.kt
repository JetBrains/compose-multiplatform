package com.jetbrains.compose.widgets

import androidx.compose.desktop.DesktopTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.Text
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyScrollable() {
    MaterialTheme {
        DesktopTheme {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(color = Color(180, 180, 180))
                    .padding(10.dp)
            ) {

                val state = rememberLazyListState()
                val itemCount = 100

                LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
                    items((1..itemCount).toList()) { x ->
                        TextBox("Item in ScrollableColumn #$x")
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = state,
                        itemCount = itemCount,
                        averageItemSize = 37.dp // TextBox height + Spacer height
                    )
                )
            }
        }
    }
}

@Composable
private fun TextBox(text: String = "Item") {
    Box(
        modifier = Modifier.height(32.dp)
            .fillMaxWidth()
            .background(color = Color(0, 0, 0, 20))
            .padding(start = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text)
    }
}