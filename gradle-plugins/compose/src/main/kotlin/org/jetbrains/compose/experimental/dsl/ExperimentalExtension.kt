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
    val web: ExperimentalWebExtension = objects.newInstance(ExperimentalWebExtension::class.java)
    fun web(action: Action<ExperimentalWebExtension>) {
        action.execute(web)
    }

    val uikit: ExperimentalUiKitExtension = objects.newInstance(ExperimentalUiKitExtension::class.java)
    fun uikit(action: Action<ExperimentalUiKitExtension>) {
        action.execute(uikit)
    }
}