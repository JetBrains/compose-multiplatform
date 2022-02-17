/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import kotlinx.serialization.json.Json
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

val json = Json {
    ignoreUnknownKeys = true
}

internal fun AbstractComposeIosTask.getSimctlListData(): SimctlListData {
    lateinit var simctlResult: SimctlListData
    runExternalTool(
        MacUtils.xcrun, listOf("simctl", "list", "--json"),
        processStdout = { stdout ->
            simctlResult = json.decodeFromString(SimctlListData.serializer(), stdout)
        }
    )
    return simctlResult
}
