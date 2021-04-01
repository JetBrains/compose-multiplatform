/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.desktop.application.dsl.Application
import javax.inject.Inject

abstract class DesktopExtension @Inject constructor(private val objectFactory: ObjectFactory) : ExtensionAware {
    internal var _isApplicationInitialized = false
        private set

    val application: Application by lazy {
        _isApplicationInitialized = true
        objectFactory.newInstance(Application::class.java, "main")
    }

    fun application(fn: Action<Application>) {
        fn.execute(application)
    }
}
