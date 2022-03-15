/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.compose.internal.requiredDslProperty
import javax.inject.Inject

@Suppress("unused")
abstract class ExperimentalUiKitApplication @Inject constructor(
    val name: String,
    val objects: ObjectFactory
) {
    var bundleIdPrefix: String by requiredDslProperty("require property [bundleIdPrefix] in uikit.application { ...")
    var projectName: String by requiredDslProperty("require property [projectName] in uikit.application { ...")
    val configurations: List<UiKitConfiguration> = listOf(
        UiKitConfiguration("Debug"),
        UiKitConfiguration("Release"),
    )

    val deployConfigurations: IosDeployConfigurations = objects.newInstance(IosDeployConfigurations::class.java)
    fun deployConfigurations(fn: Action<IosDeployConfigurations>) {
        fn.execute(deployConfigurations)
    }
}
