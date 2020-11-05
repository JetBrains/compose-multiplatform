package org.jetbrains.compose.desktop

import androidx.compose.runtime.Composable
import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.TextField
import androidx.compose.material.Button
import androidx.compose.foundation.Text
import org.jetbrains.compose.desktop.browser.BrowserState
import org.jetbrains.compose.desktop.browser.CefView

fun main() {
    Window("CEF-compose", IntSize(800, 800)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.DarkGray
        ) {
            Column {
                AddressBar()
                Spacer(Modifier.height(10.dp))
                WebView()
            }
        }
    }
}

@Composable
private fun AddressBar() {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .preferredHeight(58.dp)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 0.dp)
    ) {
        Row {
            TextField(
                backgroundColor = Color.White,
                activeColor = Color.DarkGray,
                inactiveColor = Color.DarkGray,
                value = BrowserState.url.value,
                onValueChange = {
                    BrowserState.url.value = it 
                },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                label = { }
            )
            Spacer(Modifier.width(10.dp))
            Button(
                modifier = Modifier.preferredHeight(48.dp),
                shape = CircleShape,
                onClick = { BrowserState.loadURL(BrowserState.url.value) }
            ) {
                Text(text = "Go!")
            }
        }
    }
}

@Composable
private fun WebView() {
    Surface(
        color = Color.Gray,
        modifier = Modifier.fillMaxSize().padding(10.dp)
    ) {
        if (BrowserState.isReady()) {
            CefView()
        }
    }
}
