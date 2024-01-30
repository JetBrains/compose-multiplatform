/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.service

import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

// a hack to get BuildEventsListenerRegistry conveniently, which can only be injected by Gradle
@Suppress("UnstableApiUsage")
internal abstract class BuildEventsListenerRegistryProvider @Inject constructor(val registry: BuildEventsListenerRegistry) {
    companion object {
        fun getInstance(project: Project): BuildEventsListenerRegistry =
            project.objects.newInstance(BuildEventsListenerRegistryProvider::class.java).registry
    }
}