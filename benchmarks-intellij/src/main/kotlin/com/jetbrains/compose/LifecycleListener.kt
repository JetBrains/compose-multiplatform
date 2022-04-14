/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.compose.benchmark.BenchmarkToolWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Component
import java.awt.Graphics
import java.nio.file.Files
import javax.swing.Icon

private val ANCHORS = listOf(ToolWindowAnchor.BOTTOM, ToolWindowAnchor.LEFT, ToolWindowAnchor.RIGHT)
private val SIDE_TOOLS = listOf(true, false)

class LifecycleListener : com.intellij.ide.AppLifecycleListener {

    val swingScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    init {
        swingScope.launch {
            invokeLater {
                val tempDir = Files.createTempDirectory("idea_project")
                val emptyProject: Project = ProjectUtil.openOrImport(tempDir)
                val toolWindowManager = ToolWindowManager.getInstance(emptyProject)
                val ids: MutableList<String> = mutableListOf()
                for (anchor in ANCHORS) {
                    for (sideTool in SIDE_TOOLS) {
                        toolWindowManager.registerToolWindow(
                            RegisterToolWindowTask(
                                id = "Compose${ids.size}".also { ids.add(it) },
                                anchor = anchor,
                                component = null,
                                sideTool = sideTool,
                                canCloseContent = true,
                                canWorkInDumbMode = true,
                                icon = null,
                                shouldBeAvailable = true,
                                contentFactory = BenchmarkToolWindow(),
                            )
                        )
                    }
                }
            }
        }
    }

}
