/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import com.jetbrains.compose.theme.WidgetTheme
import org.intellij.datavis.r.inlays.components.GraphicsManager

@Composable
fun IntellijTheme(project: Project, content: @Composable () -> Unit) {
    val isDarkMode = try {
        GraphicsManager.getInstance(project)?.isDarkModeEnabled ?: false
    } catch (t: Throwable) {
        false
    }
    WidgetTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
