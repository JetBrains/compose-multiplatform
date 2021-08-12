/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.runTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import javax.swing.SwingUtilities

class RunPreviewAction(
    private val previewLocation: PreviewLocation
) : AnAction({ "Show non-interactive preview" }, PreviewIcons.RUN_PREVIEW) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        buildPreviewViaGradle(project, previewLocation)
    }
}

internal const val PREVIEW_EDITOR_TOOLBAR_GROUP_ID = "Compose.Desktop.Preview.Editor.Toolbar"

class RefreshOrRunPreviewAction : AnAction(PreviewIcons.COMPOSE) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val previewLocation = ReadAction.compute<PreviewLocation?, Throwable> {
            val editor = e.dataContext.getData(CommonDataKeys.EDITOR)
            if (editor != null) {
                e.presentation.isEnabled = false
                parentPreviewAtCaretOrNull(editor)
            } else null
        }
        if (previewLocation != null) {
            buildPreviewViaGradle(project, previewLocation)
        }
    }
}

private fun buildPreviewViaGradle(project: Project, previewLocation: PreviewLocation) {
    val previewToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Desktop Preview")
    previewToolWindow?.setAvailable(true)

    val gradleVmOptions = GradleSettings.getInstance(project).gradleVmOptions
    val settings = ExternalSystemTaskExecutionSettings()
    settings.executionName = "Preview: ${previewLocation.fqName}"
    settings.externalProjectPath = previewLocation.modulePath
    settings.taskNames = listOf(previewLocation.taskName)
    settings.vmOptions = gradleVmOptions
    settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
    val previewService = project.service<PreviewStateService>()
    val gradleCallbackPort = previewService.gradleCallbackPort
    settings.scriptParameters =
        listOf(
            "-Pcompose.desktop.preview.target=${previewLocation.fqName}",
            "-Pcompose.desktop.preview.ide.port=$gradleCallbackPort"
        ).joinToString(" ")
    SwingUtilities.invokeLater {
        ToolWindowManager.getInstance(project).getToolWindow("Desktop Preview")?.activate {
            previewService.buildStarted()
        }
    }
    runTask(
        settings,
        DefaultRunExecutor.EXECUTOR_ID,
        project,
        GradleConstants.SYSTEM_ID,
        object : TaskCallback {
            override fun onSuccess() {
                previewService.buildFinished(success = true)
            }
            override fun onFailure() {
                previewService.buildFinished(success = false)
            }
        },
        ProgressExecutionMode.IN_BACKGROUND_ASYNC,
        false,
        UserDataHolderBase()
    )
}
