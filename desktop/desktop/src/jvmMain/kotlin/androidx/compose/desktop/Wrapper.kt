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
package androidx.compose.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwners
import androidx.compose.ui.platform.setContent

/**
 * Sets Compose content of the ComposeWindow.
 *
 * @param content Composable content of the ComposeWindow.
 *
 * @return Composition of the content.
 */
fun ComposeWindow.setContent(content: @Composable () -> Unit): Composition {
    return this.layer.setContent(
        parent = parent,
        invalidate = this::needRedrawLayer,
        content = content
    )
}

/**
 * Sets Compose content of the ComposePanel.
 *
 * @param content Composable content of the ComposePanel.
 *
 * @return Composition of the content.
 */
fun ComposePanel.setContent(content: @Composable () -> Unit): Composition {
    return this.layer.setContent(
        parent = this,
        invalidate = this::needRedrawLayer,
        content = content
    )
}

internal fun ComposeLayer.setContent(
    parent: Any? = null,
    invalidate: () -> Unit = this::needRedrawLayer,
    content: @Composable () -> Unit
): Composition {
    check(owners == null) {
        "Cannot setContent twice."
    }
    val owners = DesktopOwners(this.wrapped, invalidate)
    val owner = DesktopOwner(owners, density)
    this.owners = owners
    val composition = owner.setContent(content)

    onDensityChanged(owner::density::set)

    when (parent) {
        is AppFrame -> parent.onDismissEvents.add(owner::dispose)
        is ComposePanel -> parent.onDispose = owner::dispose
    }

    return composition
}
