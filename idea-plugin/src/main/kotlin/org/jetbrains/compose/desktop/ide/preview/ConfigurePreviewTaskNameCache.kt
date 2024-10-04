/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.Locale

internal val DEFAULT_CONFIGURE_PREVIEW_TASK_NAME = "configureDesktopPreview"

internal interface ConfigurePreviewTaskNameProvider {
    @RequiresReadLock
    fun configurePreviewTaskNameOrNull(module: Module):  String?
}

internal class ConfigurePreviewTaskNameProviderImpl : ConfigurePreviewTaskNameProvider {
    @RequiresReadLock
    override fun configurePreviewTaskNameOrNull(module: Module): String? {
        val modulePath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
        val moduleNode = moduleDataNodeOrNull(module.project, modulePath)
        if (moduleNode != null) {
            val target = ExternalSystemApiUtil.getChildren(moduleNode, kotlinTargetDataKey).singleOrNull()
            if (target != null) {
                return previewTaskName(target.data.externalName)
            }
        }

        return null
    }

    private fun previewTaskName(targetName: String = ""): String {
        val capitalizedTargetName =
            targetName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return "$DEFAULT_CONFIGURE_PREVIEW_TASK_NAME$capitalizedTargetName"
    }

    private fun moduleDataNodeOrNull(project: Project, modulePath: String): DataNode<ModuleData>? {
        val projectDataManager = ProjectDataManager.getInstance()
        for (settings in GradleSettings.getInstance(project).linkedProjectsSettings) {
            val projectInfo = projectDataManager.getExternalProjectData(project, GradleConstants.SYSTEM_ID, settings.externalProjectPath)
            val projectNode = projectInfo?.externalProjectStructure ?: continue
            val moduleNodes = ExternalSystemApiUtil.getChildren(projectNode, ProjectKeys.MODULE)
            for (moduleNode in moduleNodes) {
                val externalProjectPath = moduleNode.data.linkedExternalProjectPath
                if (externalProjectPath == modulePath) {
                    return moduleNode
                }
            }
        }
        return null
    }
}

internal class ConfigurePreviewTaskNameCache(
    private val provider: ConfigurePreviewTaskNameProvider
) : ConfigurePreviewTaskNameProvider {
    private var cachedModuleId: String? = null
    private var cachedTaskName: String? = null

    @RequiresReadLock
    override fun configurePreviewTaskNameOrNull(module: Module): String? {
        val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
        val moduleId = "$externalProjectPath#${module.name}"

        synchronized(this) {
            if (moduleId == cachedModuleId) return cachedTaskName
        }

        val taskName = provider.configurePreviewTaskNameOrNull(module)
        synchronized(this) {
            cachedTaskName = taskName
            cachedModuleId = moduleId
        }
        return taskName
    }

    fun invalidate() {
        synchronized(this) {
            cachedModuleId = null
            cachedTaskName = null
        }
    }
}
