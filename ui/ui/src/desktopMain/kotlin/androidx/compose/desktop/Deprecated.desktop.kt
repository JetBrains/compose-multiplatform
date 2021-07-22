/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.NoOpUpdate
import androidx.compose.ui.graphics.Color
import java.awt.Component

@Deprecated(
    "Use androidx.compose.ui.awt.ComposeWindow",
    replaceWith = ReplaceWith(
        "ComposeWindow",
        "androidx.compose.ui.awt.ComposeWindow"
    )
)
typealias ComposeWindow = androidx.compose.ui.awt.ComposeWindow

@Deprecated(
    "Use androidx.compose.ui.awt.ComposeDialog",
    replaceWith = ReplaceWith(
        "ComposeDialog",
        "androidx.compose.ui.awt.ComposeDialog"
    )
)
typealias ComposeDialog = androidx.compose.ui.awt.ComposeDialog

@Deprecated(
    "Use androidx.compose.ui.awt.ComposePanel",
    replaceWith = ReplaceWith(
        "ComposePanel",
        "androidx.compose.ui.awt.ComposePanel"
    )
)
typealias ComposePanel = androidx.compose.ui.awt.ComposePanel

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory]
 * block will be called to obtain the [Component] to be composed. The Swing component is
 * placed on top of the Compose layer.
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be reexecuted to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
@Deprecated(
    "Use androidx.compose.ui.awt.SwingPanel",
    replaceWith = ReplaceWith(
        "SwingPanel(background, factory, modifier, update)",
        "androidx.compose.ui.awt.SwingPanel"
    )
)
fun <T : Component> SwingPanel(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate
) = androidx.compose.ui.awt.SwingPanel(
    background, factory, modifier, update
)