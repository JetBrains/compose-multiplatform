/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import java.io.IOException

@ExperimentalResourceApi
actual fun resource(path: String): Resource = AndroidResourceImpl(path)

@ExperimentalResourceApi
private class AndroidResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ByteArray {
        val classLoader = Thread.currentThread().contextClassLoader ?: (::AndroidResourceImpl.javaClass.classLoader)
        val resource = classLoader.getResourceAsStream(path)
        if (resource != null) {
            return resource.readBytes()
        } else {
            throw MissingResourceException(path)
        }
    }
}

internal actual class MissingResourceException actual constructor(path: String) :
    IOException("Missing resource with path: $path")
