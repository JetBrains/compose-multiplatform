package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.desktop.DesktopTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LazyScrollable() {
    MaterialTheme {
        DesktopTheme {
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(10.dp)
            ) {

                val state = rememberLazyListState()
                val itemCount = 100

                LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
                    items(itemCount) { x ->
                        TextBox("Item in ScrollableColumn #$x")
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = state
                    )
                )
            }
        }
    }
}

@Composable
private fun TextBox(text: String = "Item") {
    Surface(
        color = Color(135, 135, 135, 40),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.height(32.dp)
                .fillMaxWidth()
                .padding(start = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = text)
        }
    }
}