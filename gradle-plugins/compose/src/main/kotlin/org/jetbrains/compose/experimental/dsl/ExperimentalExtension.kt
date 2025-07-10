/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.compose.internal.DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION
import javax.inject.Inject

abstract class ExperimentalExtension @Inject constructor(
    objects: ObjectFactory
) {
    @Suppress("DEPRECATION")
    @Deprecated(
        message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION,
    )
    val web: ExperimentalWebExtension = objects.newInstance(ExperimentalWebExtension::class.java)

    @Suppress("DEPRECATION")
    @Deprecated(
        message = DEPRECATED_EXPERIMENTAL_MESSAGE_FOR_WEB_IN_CONFIGURATION,
    )
    fun web(action: Action<ExperimentalWebExtension>): Unit = action.execute(web)
}
