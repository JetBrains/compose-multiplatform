/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import androidx.compose.ui.awt.ComposePanel
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.compose.benchmark.BenchmarkToolWindow
import com.jetbrains.compose.benchmark.CounterPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.nio.file.Files
import java.util.function.Supplier
import javax.swing.Icon

class LifecycleListener : com.intellij.ide.AppLifecycleListener {

    val swingScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    init {
        swingScope.launch {
            invokeLater {
                val iconSize = 20
                val icon = object : Icon {
                    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                        g?.color = java.awt.Color(0xff00ff)
                        g?.fillRect(0, 0, iconSize, iconSize)
                    }
                    override fun getIconWidth(): Int = iconSize
                    override fun getIconHeight(): Int = iconSize
                }

                val tempDir = Files.createTempDirectory("idea_project")
                val emptyProject: Project = ProjectUtil.openOrImport(tempDir)
                val toolWindowManager = ToolWindowManager.getInstance(emptyProject)
                repeat(5) { index ->
                    toolWindowManager.registerToolWindow(
                        RegisterToolWindowTask(
                            id = "ComposeBottom$index",
                            anchor = ToolWindowAnchor.BOTTOM,
                            component = null,
                            sideTool = true,
                            canCloseContent = true,
                            canWorkInDumbMode = true,
                            icon = icon,
                            shouldBeAvailable = true,
                            contentFactory = BenchmarkToolWindow(),
                            stripeTitle = { "stripeTitle" }
                        )
                    )
                }

            }
        }
    }

}
