package example.todo.desktop

import LoadingState.Finished
import LoadingState.Initializing
import LoadingState.Loading
import WebView
import androidx.compose.desktop.DesktopTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import rememberWebViewNavigator
import rememberWebViewState


fun main() {
    application {
        val windowState = rememberWindowState()
        val webViewState = rememberWebViewState("https://baidu.com")
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = when (webViewState.loadingState) {
                Finished -> webViewState.pageTitle?:"WebView"
                Initializing -> "初始化成功"
                is Loading -> "加载成功${((webViewState.loadingState as Loading).progress * 100f).toInt()}%"
            }
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MaterialTheme {
                    DesktopTheme {
                        WebView(
                            state = webViewState,
                            modifier = Modifier.fillMaxSize(),
                            navigator = rememberWebViewNavigator()
                        )
                    }
                }
            }
        }
    }
}

