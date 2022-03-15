/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.internal.MacUtils
import org.jetbrains.compose.desktop.application.internal.UnixUtils
import org.jetbrains.compose.experimental.uikit.tasks.AbstractComposeIosTask

internal val Project.iosDeployExecutable get() = iosDeploySrc.resolve("build/Release/ios-deploy")

private const val IOS_DEPLOY_GIT = "https://github.com/ios-control/ios-deploy.git"
private const val IOS_DEPLOY_TAG = "1.11.4"
private val Project.iosDeploySrc get() = rootProject.buildDir.resolve("ios-deploy-$IOS_DEPLOY_TAG-src")

internal fun Project.configureInstallIosDeployTask() =
    tasks.composeIosTask<AbstractComposeIosTask>("iosInstallIosDeploy") {
        onlyIf { !iosDeployExecutable.exists() }
        doLast {
            iosDeploySrc.deleteRecursively()
            runExternalTool(
                UnixUtils.git,
                listOf(
                    "clone",
                    "--depth", "1",
                    "--branch", IOS_DEPLOY_TAG,
                    IOS_DEPLOY_GIT,
                    iosDeploySrc.absolutePath
                )
            )
            runExternalTool(
                MacUtils.xcodeBuild,
                listOf("-target", "ios-deploy"),
                workingDir = iosDeploySrc
            )
        }
    }
