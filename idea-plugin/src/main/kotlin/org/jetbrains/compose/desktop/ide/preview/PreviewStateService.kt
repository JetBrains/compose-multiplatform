/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.task.*
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.*
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import javax.swing.JComponent
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

@Service
class PreviewStateService(private val myProject: Project) : Disposable {
    private val previewListener = CompositePreviewListener()
    private val previewManager: PreviewManager = PreviewManagerImpl(previewListener)
    val gradleCallbackPort: Int
        get() = previewManager.gradleCallbackPort
    private val configurePreviewTaskNameCache =
        ConfigurePreviewTaskNameCache(ConfigurePreviewTaskNameProviderImpl())

    init {
        val projectRefreshListener = ConfigurePreviewTaskNameCacheInvalidator(configurePreviewTaskNameCache)
        ExternalSystemProgressNotificationManager.getInstance()
            .addNotificationListener(projectRefreshListener, myProject)
    }

    @RequiresReadLock
    internal fun configurePreviewTaskNameOrNull(module: Module): String? =
        configurePreviewTaskNameCache.configurePreviewTaskNameOrNull(module)

    override fun dispose() {
        previewManager.close()
        configurePreviewTaskNameCache.invalidate()
    }

    internal fun registerPreviewPanels(
        previewPanel: PreviewPanel,
        loadingPanel: JBLoadingPanel
    ) {
        val previewResizeListener = PreviewResizeListener(previewManager)
        previewPanel.addAncestorListener(previewResizeListener)
        Disposer.register(this) { previewPanel.removeAncestorListener(previewResizeListener) }

        previewListener.addListener(PreviewPanelUpdater(previewPanel))
        previewListener.addListener(LoadingPanelUpdater(loadingPanel))
        previewListener.addListener(object : PreviewListenerBase() {
            private val reported = hashSetOf<Pair<Int, Int>>()

            override fun onIncompatibleProtocolVersions(versionServer: Int, versionClient: Int) {
                if (reported.add(versionServer to versionClient)) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Compose MPP Notifications")
                        .createNotification("Compose Desktop Preview may be incompatible " +
                                "with provided Compose Gradle plugin. " +
                                "Please use matching versions.",
                            NotificationType.ERROR)
                        .notify(myProject)
                }
            }
        })
    }

    internal fun buildStarted() {
        previewListener.onNewBuildRequest()
    }

    internal fun buildFinished(success: Boolean) {
        previewListener.onFinishedBuild(success)
    }
}

private class PreviewResizeListener(private val previewManager: PreviewManager) : AncestorListener {
    private fun updateFrameSize(c: JComponent) {
        val frameConfig = FrameConfig(
            width = c.width,
            height = c.height,
            scale = c.graphicsConfiguration.defaultTransform.scaleX
        )
        previewManager.updateFrameConfig(frameConfig)
    }

    override fun ancestorAdded(event: AncestorEvent) {
        updateFrameSize(event.component)

    }

    override fun ancestorRemoved(event: AncestorEvent) {
    }

    override fun ancestorMoved(event: AncestorEvent) {
        updateFrameSize(event.component)
    }
}

private class PreviewPanelUpdater(private val panel: PreviewPanel) : PreviewListenerBase() {
    override fun onRenderedFrame(frame: RenderedFrame) {
        panel.previewImage(frame.image, frame.dimension)
    }
}

private class LoadingPanelUpdater(private val panel: JBLoadingPanel) : PreviewListenerBase() {
    override fun onNewBuildRequest() {
        panel.setLoadingText("Building project")
        panel.startLoading()
    }

    override fun onFinishedBuild(success: Boolean) {
        panel.stopLoading()
    }

    override fun onNewRenderRequest(previewRequest: FrameRequest) {
        panel.setLoadingText("Rendering preview")
        panel.startLoading()
    }

    override fun onRenderedFrame(frame: RenderedFrame) {
        panel.stopLoading()
    }
}

// ExternalSystemTaskNotificationListenerAdapter is used,
// because ExternalSystemTaskNotificationListener interface's API
// was changed between 2020.3 and 2021.1, so a direct implementation
// would not work with both 2020.3 and 2021.1
private class ConfigurePreviewTaskNameCacheInvalidator(
    private val configurePreviewTaskNameCache: ConfigurePreviewTaskNameCache
) : ExternalSystemTaskNotificationListenerAdapter(null) {
    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        if (
            id.projectSystemId == GRADLE_SYSTEM_ID &&
            id.type == ExternalSystemTaskType.RESOLVE_PROJECT
        ) {
            configurePreviewTaskNameCache.invalidate()
        }
    }
}