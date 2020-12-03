package org.jetbrains.compose.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
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
    val browser = BrowserState()
    val url = mutableStateOf("https://www.google.com")

    Window(
        title = "CEF-compose",
        size = IntSize(800, 800),
        events = WindowEvents(
            onFocusGet = { browser.loadURL(url.value) }
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.DarkGray
        ) {
            Column {
                AddressBar(browser, url)
                Spacer(Modifier.height(10.dp))
                WebView(browser)
            }
        }
    }
}

@Composable
private fun AddressBar(browser: BrowserState, url: MutableState<String>) {
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
                value = url.value,
                onValueChange = {
                    url.value = it 
                },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                label = { }
            )
            Spacer(Modifier.width(10.dp))
            Button(
                modifier = Modifier.preferredHeight(48.dp),
                shape = CircleShape,
                onClick = { browser.loadURL(url.value) }
            ) {
                Text(text = "Go!")
            }
        }
    }
}

@Composable
private fun WebView(browser: BrowserState) {
    Surface(
        color = Color.Gray,
        modifier = Modifier.fillMaxSize().padding(10.dp)
    ) {
        CefView(browser)
    }
}
