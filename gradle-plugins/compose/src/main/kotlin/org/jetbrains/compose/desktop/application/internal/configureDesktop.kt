/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.tasks.AbstractUnpackDefaultComposeApplicationResourcesTask
import org.jetbrains.compose.internal.registerTask

internal fun configureDesktop(project: Project, desktopExtension: DesktopExtension) {
    val unpackDefaultResources = lazy {
        project.registerTask<AbstractUnpackDefaultComposeApplicationResourcesTask>(
            "unpackDefaultComposeDesktopApplicationResources"
        ) {}
    }

    if (desktopExtension._isJvmApplicationInitialized) {
        configureJvmApplication(project, desktopExtension.application, unpackDefaultResources.value)
    }

    if (desktopExtension._isNativeApplicationInitialized) {
        configureNativeApplication(project, desktopExtension.nativeApplication, unpackDefaultResources.value)
    }
}