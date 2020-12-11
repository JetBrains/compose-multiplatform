package org.jetbrains.compose.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.layout.Box
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
import org.jetbrains.compose.desktop.browser.Browser
import org.jetbrains.compose.desktop.browser.BrowserView
import org.jetbrains.compose.desktop.browser.BrowserSlicer

fun main(args: Array<String>) {
    val browser = when {
        args.isEmpty() -> BrowserView()
        args[0] == "slices" -> BrowserSlicer(IntSize(800, 700))
        else -> {
            BrowserView()
        }
    }

    val url = mutableStateOf("https://www.google.com")

    Window(
        title = "CEF-compose",
        size = IntSize(900, 900),
        events = WindowEvents(
            onFocusGet = { browser.load(url.value) }
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
private fun AddressBar(browser: Browser, url: MutableState<String>) {
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
                onClick = { browser.load(url.value) }
            ) {
                Text(text = "Go!")
            }
        }
    }
}

@Composable
private fun WebView(browser: Browser) {
    Surface(
        color = Color.Gray,
        modifier = Modifier.fillMaxSize().padding(10.dp)
    ) {
        when (browser) {
            is BrowserView -> {
                browser.view()
            }
            is BrowserSlicer -> {
                Column {
                    browser.slice(0, 200)
                    Spacer(Modifier.height(30.dp))
                    browser.slice(200, 200)
                    Spacer(Modifier.height(30.dp))
                    browser.tail()
                }
            }
        }
    }
}
