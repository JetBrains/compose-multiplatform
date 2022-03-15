/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.UnixUtils
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

internal val Project.xcodeGenExecutable get() = xcodeGenSrc.resolve(".build/apple/Products/Release/xcodegen")

private const val XCODE_GEN_GIT = "https://github.com/yonaskolb/XcodeGen.git"
private const val XCODE_GEN_TAG = "2.26.0"
private val Project.xcodeGenSrc get() = rootProject.buildDir.resolve("xcodegen-$XCODE_GEN_TAG-src")

fun Project.configureInstallXcodeGenTask() =
    tasks.composeIosTask<AbstractComposeIosTask>("iosInstallXcodeGen") {
        onlyIf { !xcodeGenExecutable.exists() }
        doLast {
            xcodeGenSrc.deleteRecursively()
            runExternalTool(
                UnixUtils.git,
                listOf(
                    "clone",
                    "--depth", "1",
                    "--branch", XCODE_GEN_TAG,
                    XCODE_GEN_GIT,
                    xcodeGenSrc.absolutePath
                )
            )
            runExternalTool(
                MacUtils.make,
                listOf("build"),
                workingDir = xcodeGenSrc
            )
        }
    }
