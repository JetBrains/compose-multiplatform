/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

internal fun Project.configureDeleteUnavailableSimulatorTask() =
    tasks.composeIosTask<AbstractComposeIosTask>("iosDeleteUnavailableSimulator") {
        doLast {
            runExternalTool(
                MacUtils.xcrun,
                listOf("simctl", "delete", "unavailable")
            )
        }
    }
