/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.copypasta

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.newtext.text.copypasta.selection.SelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.collect

@Composable
internal actual fun ContextMenuArea(
    manager: SelectionManager,
    content: @Composable () -> Unit
) {
    /* noop*/
}

@Composable
internal fun OpenMenuAdjuster(state: ContextMenuState, adjustAction: (Offset) -> Unit) {
    LaunchedEffect(state) {
        snapshotFlow { state.status }.collect { status ->
            if (status is ContextMenuState.Status.Open) {
                adjustAction(status.rect.center)
            }
        }
    }
}

@Composable
internal fun SelectionManager.contextMenuItems(): () -> List<ContextMenuItem> {
    return {
        listOf()
    }
}