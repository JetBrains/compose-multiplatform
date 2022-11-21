/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

/**
 * Should implement equals() and hashCode()
 */
@ExperimentalResourceApi
interface Resource {
    suspend fun readBytes(): LoadState<ByteArray> //todo in future use streaming
}

/**
 * Get resource from sourceSet/resources.
 * For examples commonMain/resources
 */
@ExperimentalResourceApi
expect fun resource(path: String): Resource

//todo reuse LoadState.Error
