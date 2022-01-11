/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.dsl

import javax.inject.Inject

abstract class ExperimentalWebApplication  @Inject constructor(
    @Suppress("unused")
    val name: String,
) {

}