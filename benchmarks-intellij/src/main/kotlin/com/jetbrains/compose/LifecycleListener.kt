/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.compose.benchmark.BenchmarkToolWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.nio.file.Files
import kotlin.random.Random

private val ANCHORS = listOf(ToolWindowAnchor.LEFT, ToolWindowAnchor.BOTTOM, ToolWindowAnchor.RIGHT)
private val SIDE_TOOLS = listOf(true, false)
private val COUNT = ANCHORS.size * SIDE_TOOLS.size

class LifecycleListener : com.intellij.ide.AppLifecycleListener {

    private val swingScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private lateinit var tempProject: Project
    private val toolWindowIds: MutableList<String> = mutableListOf()

    init {
        swingScope.launch {
            invokeLater {
                val tempDir = Files.createTempDirectory("idea_project")
                tempProject = ProjectUtil.openOrImport(tempDir)
                val toolWindowManager = ToolWindowManager.getInstance(tempProject)
                for (anchor in ANCHORS) {
                    for (sideTool in SIDE_TOOLS) {
                        val id = "Compose${toolWindowIds.size}"
                        toolWindowIds.add(id)
                        val toolWindow: ToolWindow = toolWindowManager.registerToolWindow(
                            RegisterToolWindowTask(
                                id = id,
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
            while (true) {
                try {
                    delay(500)
                    val toolWindowManager = ToolWindowManager.getInstance(tempProject)
                    val toolWindows = toolWindowIds.map {
                        toolWindowManager.getToolWindow(it)
                    }.filterNotNull()
                    stressTestToolWindows(toolWindows)
                } catch (t: Throwable) {
                    // do nothing
                }
            }
        }
    }

}

suspend fun stressTestToolWindows(toolWindows: List<ToolWindow>) {
    val runtime = Runtime.getRuntime()
    fun printLogs(message: String) {
        runtime.gc()
        val memory = runtime.totalMemory() - runtime.freeMemory()
        val memoryStr = String.format("%.2f MB", memory / 1e6f)
        println("-- $message --")
        println("-- Used memory: $memoryStr")
        println("-- ")
    }
    while (true) {
        val visiblePanelsCount = toolWindows.count { it.isVisible }
        printLogs("$visiblePanelsCount panels")
        delay(500)
        toolWindows.forEach {
            if (Random.nextBoolean()) {
                if (it.isVisible.not()) {
                    it.show()
                }
            } else {
                if (it.isVisible) {
                    it.hide()
                }
            }
        }
    }
}
