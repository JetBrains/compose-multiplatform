/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class ExperimentalUiKitExtension @Inject constructor(
    objectFactory: ObjectFactory
) {
    internal var _isApplicationInitialized = false
        private set

    val application: ExperimentalUiKitApplication by lazy {
        _isApplicationInitialized = true
        objectFactory.newInstance(ExperimentalUiKitApplication::class.java, "main")
    }

    fun application(fn: Action<ExperimentalUiKitApplication>) {
        fn.execute(application)
    }
}

