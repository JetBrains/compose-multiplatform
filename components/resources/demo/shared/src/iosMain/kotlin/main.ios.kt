/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import org.jetbrains.compose.resources.demo.shared.UseResources

fun MainViewController() =
    ComposeUIViewController {
        Column {
            Box(
                modifier = Modifier
                    .height(100.dp)
            )
            UseResources()
        }
    }
