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
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.LayoutNode

/**
 * Composes the given composable into [SkiaBasedOwner]
 *
 * @param parent The parent composition reference to coordinate scheduling of composition updates
 *        If null then default root composition will be used.
 * @param getCompositionLocalContext getter for retrieving the top-level composition local context.
 * Can be backed by `mutableStateOf` to dynamically change top-level locals.
 * @param content A `@Composable` function declaring the UI contents
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun SkiaBasedOwner.setContent(
    parent: CompositionContext,
    getCompositionLocalContext: () -> CompositionLocalContext? = { null },
    content: @Composable () -> Unit
): Composition {
    val composition = Composition(DefaultUiApplier(root), parent)
    val owner = this
    composition.setContent {
        getCompositionLocalContext().provide {
            ProvideCommonCompositionLocals(
                owner = owner,
                uriHandler = remember { PlatformUriHandler() },
                content = content
            )
            LaunchedEffect(owner) { accessibilityController.syncLoop() }
        }
    }
    return composition
}

@Composable
private fun CompositionLocalContext?.provide(content: @Composable () -> Unit) {
    if (this != null) {
        CompositionLocalProvider(this, content = content)
    } else {
        content()
    }
}

internal actual fun createSubcomposition(
    container: LayoutNode,
    parent: CompositionContext
): Composition = Composition(
    DefaultUiApplier(container),
    parent
)
