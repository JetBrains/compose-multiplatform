/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

interface PreviewListener {
    fun onNewBuildRequest()
    fun onFinishedBuild(success: Boolean)
    fun onNewRenderRequest(previewRequest: FrameRequest)
    fun onRenderedFrame(frame: RenderedFrame)
    fun onError(error: String)
}

open class PreviewListenerBase : PreviewListener {
    override fun onNewBuildRequest() {}
    override fun onFinishedBuild(success: Boolean) {}

    override fun onNewRenderRequest(previewRequest: FrameRequest) {}
    override fun onRenderedFrame(frame: RenderedFrame) {}

    override fun onError(error: String) {}
}

class CompositePreviewListener : PreviewListener {
    private val listeners = arrayListOf<PreviewListener>()

    override fun onNewBuildRequest() {
        forEachListener { it.onNewBuildRequest() }
    }

    override fun onFinishedBuild(success: Boolean) {
        forEachListener { it.onFinishedBuild(success) }
    }

    override fun onNewRenderRequest(previewRequest: FrameRequest) {
        forEachListener { it.onNewRenderRequest(previewRequest) }
    }

    override fun onRenderedFrame(frame: RenderedFrame) {
        forEachListener { it.onRenderedFrame(frame) }
    }

    override fun onError(error: String) {
        forEachListener { it.onError(error) }
    }

    @Synchronized
    fun addListener(listener: PreviewListener) {
        listeners.add(listener)
    }

    @Synchronized
    private fun forEachListener(fn: (PreviewListener) -> Unit) {
        listeners.forEach(fn)
    }
}
