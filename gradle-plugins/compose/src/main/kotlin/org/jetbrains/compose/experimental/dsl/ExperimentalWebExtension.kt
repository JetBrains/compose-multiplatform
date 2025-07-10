/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.internal.DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION
import javax.inject.Inject

@Deprecated(
    message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION,
)
abstract class ExperimentalWebExtension @Inject constructor(private val objectFactory: ObjectFactory) : ExtensionAware {
    @Suppress("DEPRECATION")
    @Deprecated(
        message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION,
    )
    val application: ExperimentalWebApplication by lazy {
        objectFactory.newInstance(ExperimentalWebApplication::class.java, "main")
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION,
    )
    fun application(fn: Action<ExperimentalWebApplication>): Unit = fn.execute(application)
}
