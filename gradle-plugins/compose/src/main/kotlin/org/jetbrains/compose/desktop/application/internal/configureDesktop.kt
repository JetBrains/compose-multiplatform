/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.tasks.AbstractUnpackDefaultComposeApplicationResourcesTask
import org.jetbrains.compose.internal.utils.registerTask

internal fun configureDesktop(project: Project, desktopExtension: DesktopExtension) {
    if (desktopExtension._isJvmApplicationInitialized) {
        val appInternal = desktopExtension.application as JvmApplicationInternal
        val defaultBuildType = appInternal.data.buildTypes.default
        val appData = JvmApplicationContext(project, appInternal, defaultBuildType)
        appData.configureJvmApplication()
    }

    if (desktopExtension._isNativeApplicationInitialized) {
        val unpackDefaultResources = project.registerTask<AbstractUnpackDefaultComposeApplicationResourcesTask>(
            "unpackDefaultComposeDesktopNativeApplicationResources"
        ) {}
        configureNativeApplication(project, desktopExtension.nativeApplication, unpackDefaultResources)
    }

    project.configureHotReload()
}