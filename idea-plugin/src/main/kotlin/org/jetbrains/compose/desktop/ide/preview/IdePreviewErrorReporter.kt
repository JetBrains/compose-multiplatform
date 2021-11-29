/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.PreviewErrorReporter

internal class IdePreviewErrorReporter(
    private val logger: Logger,
    private val previewStateService: PreviewStateService
) : PreviewErrorReporter {
    override fun report(e: Throwable, details: String?) {
        report(e.stackTraceToString(), details)
    }

    override fun report(e: String, details: String?) {
        if (details != null) {
            logger.error(e, details)
        } else {
            logger.error(e)
        }
        previewStateService.clearPreviewOnError()
    }
}