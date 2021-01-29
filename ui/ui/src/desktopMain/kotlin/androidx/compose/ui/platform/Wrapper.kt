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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.LayoutNode

/**
 * Composes the given composable into [DesktopOwner]
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 *        If null then default root composition will be used.
 * @param content A `@Composable` function declaring the UI contents
 */
@OptIn(ExperimentalComposeApi::class)
internal fun DesktopOwner.setContent(
    parent: CompositionReference? = null,
    content: @Composable () -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val composition = Composition(root, DesktopUiApplier(root), parent ?: container.recomposer)
    composition.setContent {
        ProvideDesktopAmbients(this) {
            content()
        }
    }

    keyboard?.setShortcut(copyToClipboardKeySet) {
        selectionTracker.getSelectedText?.invoke()?.let {
            clipboardManager.setText(it)
        }
    }

    return composition
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProvideDesktopAmbients(owner: DesktopOwner, content: @Composable () -> Unit) {
    Providers(
        DesktopOwnersAmbient provides owner.container,
        SelectionTrackerAmbient provides owner.selectionTracker
    ) {
        ProvideCommonCompositionLocals(
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
): Composition = Composition(
    container,
    DesktopUiApplier(container),
    parent
).apply {
    setContent(composable)
}