/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.compose.foundation.demos.text

import android.app.Activity
import android.content.ContextWrapper
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private enum class ScrollableType {
    ScrollableColumn,
    LazyColumn
}

@Composable
fun TextFieldsInScrollableDemo() {
    var adjustResize by remember { mutableStateOf(false) }
    var decorFitsSystemWindows by remember { mutableStateOf(true) }
    var scrollableType by remember { mutableStateOf(ScrollableType.values().first()) }

    @Suppress("DEPRECATION")
    SoftInputMode(
        mode = if (adjustResize) SOFT_INPUT_ADJUST_RESIZE else SOFT_INPUT_ADJUST_PAN,
        decorFitsSystemWindows = decorFitsSystemWindows
    )

    Column(Modifier.windowInsetsPadding(WindowInsets.ime)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ADJUST_PAN")
            Switch(adjustResize, onCheckedChange = { adjustResize = it })
            Text("ADJUST_RESIZE")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Decor fits system windows: ")
            Checkbox(decorFitsSystemWindows, onCheckedChange = { decorFitsSystemWindows = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scrollable column")
            Switch(
                checked = scrollableType == ScrollableType.LazyColumn,
                onCheckedChange = {
                    scrollableType = if (it) {
                        ScrollableType.LazyColumn
                    } else {
                        ScrollableType.ScrollableColumn
                    }
                })
            Text("LazyColumn")
        }

        when (scrollableType) {
            ScrollableType.ScrollableColumn -> TextFieldInScrollableColumn()
            ScrollableType.LazyColumn -> TextFieldInLazyColumn()
        }
    }
}

@Composable
fun TextFieldInScrollableColumn() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        repeat(100) { index ->
            DemoTextField(index)
        }
    }
}

@Composable
fun TextFieldInLazyColumn() {
    LazyColumn {
        items(100) { index ->
            DemoTextField(index)
        }
    }
}

@Composable
private fun DemoTextField(index: Int) {
    var text by rememberSaveable { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        leadingIcon = { Text(index.toString()) },
        modifier = Modifier
            .padding(4.dp)
            .border(1.dp, Color.Black)
            .fillMaxWidth()
    )
}

/**
 * Sets the window's [softInputMode][android.view.Window.setSoftInputMode] to [mode] as long as this
 * function is composed.
 */
@Composable
private fun SoftInputMode(mode: Int, decorFitsSystemWindows: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, mode, decorFitsSystemWindows) {
        val activity = generateSequence(context) { (context as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
            ?: return@DisposableEffect onDispose {}
        val originalMode = activity.window.attributes.softInputMode
        activity.window.setSoftInputMode(mode)
        WindowCompat.setDecorFitsSystemWindows(activity.window, decorFitsSystemWindows)
        onDispose {
            activity.window.setSoftInputMode(originalMode)
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        }
    }
}