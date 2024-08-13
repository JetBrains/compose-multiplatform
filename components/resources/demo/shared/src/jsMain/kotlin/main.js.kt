/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.demo.shared.UseResources
import org.jetbrains.skiko.wasm.onWasmReady


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    configureWebResources {
        // Not necessary - It's the same as the default. We add it here just to present this feature.
        resourcePathMapping { path -> "./$path" }
    }
    onWasmReady {
        CanvasBasedWindow("Resources demo") {
            UseResources()
        }
    }
}
