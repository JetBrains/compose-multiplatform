/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class ExperimentalExtension @Inject constructor(
    objects: ObjectFactory
) {

    @Deprecated(
        message = "Starting from 1.6.10, Compose for Web goes to Alpha. Experimental configuration is not needed anymore.",
    )
    val web: ExperimentalWebExtension = objects.newInstance(ExperimentalWebExtension::class.java)

    @Deprecated(
        message = "Starting from 1.6.10, Compose for Web goes to Alpha. Experimental configuration is not needed anymore."
    )
    fun web(action: Action<ExperimentalWebExtension>) {
        action.execute(web)
    }
}