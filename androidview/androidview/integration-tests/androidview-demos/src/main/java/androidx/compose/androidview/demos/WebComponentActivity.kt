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

package androidx.compose.androidview.demos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.androidview.WebComponent
import androidx.compose.androidview.WebContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent

open class WebComponentActivity : ComponentActivity() {

    val webContext = WebContext()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            if (WebContext.debug) {
                Log.e("WebCompAct", "setContent")
            }

            RenderViews(webContext = webContext)
        }
    }

    override fun onBackPressed() {
        if (webContext.canGoBack()) {
            webContext.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
private fun RenderViews(webContext: WebContext) {
    if (WebContext.debug) {
        Log.d("WebCompAct", "renderViews")
    }

    var url by remember { mutableStateOf("https://www.google.com") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { webContext.goBack() }) {
                Text("<")
            }
            OutlinedButton(onClick = { webContext.goForward() }) {
                Text(">")
            }
            var inputUrl by remember { mutableStateOf("https://www.google.com") }
            TextField(
                modifier = Modifier.weight(1f),
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text("Url") }
            )
            OutlinedButton(
                onClick = {
                    if (inputUrl.isNotBlank()) {
                        if (WebContext.debug) {
                            Log.d("WebCompAct", "setting url to " + inputUrl)
                        }
                        url = inputUrl
                    }
                }
            ) {
                Text("Go")
            }
        }

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: start")
        }

        WebComponent(url = url, webContext = webContext)

        if (WebContext.debug) {
            Log.d("WebCompAct", "webComponent: end")
        }
    }
}