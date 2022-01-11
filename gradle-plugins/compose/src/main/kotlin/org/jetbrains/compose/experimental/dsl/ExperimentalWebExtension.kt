/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import javax.inject.Inject

abstract class ExperimentalWebExtension @Inject constructor(private val objectFactory: ObjectFactory) : ExtensionAware {
    internal var _isApplicationInitialized = false
        private set

    val application: ExperimentalWebApplication by lazy {
        _isApplicationInitialized = true
        objectFactory.newInstance(ExperimentalWebApplication::class.java, "main")
    }

    fun application(fn: Action<ExperimentalWebApplication>) {
        fn.execute(application)
    }
}
