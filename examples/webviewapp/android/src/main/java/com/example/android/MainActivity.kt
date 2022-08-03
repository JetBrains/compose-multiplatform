package com.example.android

import WebView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import rememberWebViewNavigator
import rememberWebViewState

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val webViewState = rememberWebViewState("https://google.com")
                Surface(color = MaterialTheme.colors.background) {
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