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

package androidx.compose.desktop.examples.layout

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.delay

fun main() = application {
    Dialog(
        onCloseRequest = ::exitApplication,
        state = rememberDialogState(width = 400.dp, height = 600.dp),
        undecorated = true,
    ) {
        IterateViews(
            periodMs = 1000,
            { CustomLayoutUsage() },
            { AlertDialogUsage() },
            { BadgeBoxUsage() },
            { BottomNavigationUsage() },
            { BottomSheetScaffoldUsage() },
            { ListItemUsage() },
            { NavigationRailUsage() },
            { SnackBarUsage() },
            { TabRowUsage() },
            { HorizontalScrollersInVerticalScrollersUsage() },
            { ImageUsage() },
            { SelectionContainerUsage() },
            { TextWithInlineContentUsage() },
            { VerticalScrollbarUsage() },
            { SwingPanelUsage() },
        )
    }
}

/**
 * Iterate given [views] with [periodMs] delay
 */
@Composable
fun IterateViews(periodMs: Long, vararg views: @Composable () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    if (currentIndex < views.size) {
        views[currentIndex]()
        LaunchedEffect(currentIndex) {
            delay(periodMs)
            currentIndex++
        }
    } else {
        Text("Done")
    }
}
