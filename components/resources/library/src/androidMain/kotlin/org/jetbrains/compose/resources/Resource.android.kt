/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

@ExperimentalResourceApi
actual fun resource(path: String): Resource = AndroidResourceImpl(path)

@ExperimentalResourceApi
private class AndroidResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): ByteArray {
        val contextClassLoader = Thread.currentThread().contextClassLoader!!
        return contextClassLoader.getResourceAsStream(path).readBytes()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is AndroidResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

}
