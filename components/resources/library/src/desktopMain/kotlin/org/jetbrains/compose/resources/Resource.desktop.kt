/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import java.io.IOException

@ExperimentalResourceApi
actual fun resource(path: String): Resource = DesktopResourceImpl(path)

@ExperimentalResourceApi
private class DesktopResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): ByteArray {
        val resource = (::DesktopResourceImpl.javaClass.classLoader).getResourceAsStream(path)
        if (resource != null) {
            return resource.readBytes()
        } else {
            throw MissingResourceException(path)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is DesktopResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

internal actual class MissingResourceException actual constructor(path: String) :
    IOException("missing resource with path: $path")
