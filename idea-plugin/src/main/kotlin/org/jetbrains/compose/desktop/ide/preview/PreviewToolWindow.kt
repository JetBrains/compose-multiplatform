/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class PreviewToolWindow : ToolWindowFactory, DumbAware {
    override fun isApplicable(project: Project): Boolean {
        // todo: filter only Compose projects
        return true
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.contentManager.let { content ->
            val panel = PreviewPanel()
            content.addContent(content.factory.createContent(panel, null, false))
            project.service<PreviewStateService>().registerPreviewPanel(panel)
        }
    }
}