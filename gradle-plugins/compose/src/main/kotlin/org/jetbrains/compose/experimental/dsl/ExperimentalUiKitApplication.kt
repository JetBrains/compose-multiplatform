/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@Suppress("unused")
abstract class ExperimentalUiKitApplication @Inject constructor(
    val name: String,
    val objects: ObjectFactory
) {

}
