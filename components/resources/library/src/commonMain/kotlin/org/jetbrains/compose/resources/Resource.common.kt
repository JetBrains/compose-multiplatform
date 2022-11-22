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
    suspend fun readBytes(): Result<ByteArray> //todo in future use streaming
}

/**
 * Get a resource from <sourceSet>/resources (for example, from commonMain/resources).
 */
@ExperimentalResourceApi
expect fun resource(path: String): Resource

internal expect class MissingResourceException(path: String)
