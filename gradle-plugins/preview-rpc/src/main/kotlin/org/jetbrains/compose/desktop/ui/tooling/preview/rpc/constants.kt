/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

internal const val SOCKET_TIMEOUT_MS = 1000
internal const val DEFAULT_SLEEP_DELAY_MS = 1000L
internal const val MAX_CMD_SIZE = 8 * 1024
// 100 Mb should be enough even for 8K screenshots
internal const val MAX_BINARY_SIZE = 100 * 1024 * 1024
internal const val MAX_BUF_SIZE = 8 * 1024
internal const val PREVIEW_START_OF_STACKTRACE_MARKER = "<!--START OF COMPOSE PREVIEW PROCESS FATAL EXCEPTION--!>"
