/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.experimental.uikit.tasks.DownloadXcodeGenTask
import org.jetbrains.compose.experimental.uikit.tasks.ExtractXcodeGenTask

private const val XCODE_GEN_TAG = "2.32.0"
private const val XCODE_GEN_URL = "https://github.com/yonaskolb/XcodeGen/releases/download/$XCODE_GEN_TAG/xcodegen.zip"
private val Project.xcodeGenSrc get() = rootProject.buildDir.resolve("xcodegen-$XCODE_GEN_TAG")

fun Project.configureInstallXcodeGenTask(): TaskProvider<ExtractXcodeGenTask> {
    val downalodTask = tasks.register("iosDownloadXcodeGenBinary", DownloadXcodeGenTask::class.java) {
        it.downloadUrl.set(XCODE_GEN_URL)
        it.destFile.set(xcodeGenSrc.resolve("xcodegen-$XCODE_GEN_TAG.zip"))
    }

    return tasks.register("iosExtractXcodeGenBinary", ExtractXcodeGenTask::class.java) {
        it.from(zipTree(downalodTask.map { it.outputs.files.first() }))
        it.into(xcodeGenSrc)
        it.fileMode = 0b111100100 // chmod: 744
    }
}

