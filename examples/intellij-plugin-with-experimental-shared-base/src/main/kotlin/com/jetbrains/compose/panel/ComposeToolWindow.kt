/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.panel

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.Dimension

class ComposeToolWindow : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    ApplicationManager.getApplication().invokeLater {
      toolWindow.contentManager.addContent(
        ContentFactory.SERVICE.getInstance().createContent(
          composePanel,
          "Compose tool window",
          false
        )
      )
    }
  }

  companion object {
    val stateWithIdeLifecycle = mutableStateOf(CounterState())

    val composePanel: ComposePanel by lazy {
      val panel = ComposePanel()
      panel.apply {
        this.size = Dimension(300, 300)
        setContent {
          CounterPanel(stateWithIdeLifecycle)
        }
      }
      panel
    }
  }

}
