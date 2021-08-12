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
import com.intellij.ui.components.JBLoadingPanel
import java.awt.BorderLayout

class PreviewToolWindow : ToolWindowFactory, DumbAware {
    override fun isApplicable(project: Project): Boolean {
        // todo: filter only Compose projects
        return true
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.setIcon(PreviewIcons.COMPOSE)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.contentManager.let { content ->
            val panel = PreviewPanel()
            val loadingPanel = JBLoadingPanel(BorderLayout(), project)
            loadingPanel.add(panel, BorderLayout.CENTER)
            content.addContent(content.factory.createContent(loadingPanel, null, false))
            project.service<PreviewStateService>().registerPreviewPanels(panel, loadingPanel)
        }
    }

    // don't show the toolwindow until a preview is requested
    override fun shouldBeAvailable(project: Project): Boolean =
        false
}