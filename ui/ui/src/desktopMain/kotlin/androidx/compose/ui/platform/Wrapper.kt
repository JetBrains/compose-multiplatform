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
package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionReference
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.compositionFor
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.LayoutNode

@OptIn(ExperimentalComposeApi::class)
fun DesktopOwner.setContent(content: @Composable () -> Unit): Composition {
    GlobalSnapshotManager.ensureStarted()

    val composition = compositionFor(root, DesktopUiApplier(root), Recomposer.current())
    composition.setContent {
        ProvideDesktopAmbients(this) {
            DesktopSelectionContainer(content)
        }
    }

    keyboard?.setShortcut(copyToClipboardKeySet) {
        selectionManager.recentManager?.let { selector ->
            selector.getSelectedText()?.let {
                clipboardManager.setText(it)
            }
        }
    }

    return composition
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProvideDesktopAmbients(owner: DesktopOwner, content: @Composable () -> Unit) {
    Providers(
        DesktopOwnersAmbient provides owner.container,
        SelectionManagerTrackerAmbient provides owner.selectionManager
    ) {
        ProvideCommonAmbients(
            owner = owner,
            animationClock = owner.container.animationClock,
            uriHandler = DesktopUriHandler(),
            content = content
        )
    }
}

@OptIn(ExperimentalComposeApi::class)
internal actual fun subcomposeInto(
    container: LayoutNode,
    parent: CompositionReference,
    composable: @Composable () -> Unit
): Composition = compositionFor(
    container,
    DesktopUiApplier(container),
    parent
).apply {
    setContent(composable)
}