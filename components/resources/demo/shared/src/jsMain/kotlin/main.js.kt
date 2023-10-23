/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.demo.shared.UseResources
import org.jetbrains.compose.resources.urlResource
import org.jetbrains.skiko.wasm.onWasmReady


fun main() {

    @OptIn(ExperimentalResourceApi::class)
    configureWebResources {
        // Not necessary - It's the same as the default. We add it here just to present this feature.
        setResourceImplFactory { urlResource("./$it") }
    }
    onWasmReady {
        Window("Resources demo") {
            MainView()
        }
    }
}

@Composable
fun MainView() {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(24.dp))
        UseResources()
    }
}
