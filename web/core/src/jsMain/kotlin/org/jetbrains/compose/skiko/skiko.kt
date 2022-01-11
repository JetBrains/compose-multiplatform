/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.skiko

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeCanvas
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLCanvasElement

@Composable
fun ElementScope<HTMLCanvasElement>.skiko(block: @Composable () -> Unit) {
    DomSideEffect { canvas ->
        var skikoCanvas: ComposeCanvas? = null

        onWasmReady {
            skikoCanvas = ComposeCanvas(canvas)
            skikoCanvas?.setContent(block)
        }

        onDispose {
            skikoCanvas?.dispose()
        }
    }
}