/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

@ExperimentalResourceApi
actual fun resource(path: String): Resource = MacOSResourceImpl(path)

@ExperimentalResourceApi
private class MacOSResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): ByteArray {
        val currentDirectoryPath = NSFileManager.defaultManager().currentDirectoryPath
        val contentsAtPath: NSData = NSFileManager.defaultManager().contentsAtPath(
            "$currentDirectoryPath/src/commonMain/resources/" + path
        )!!
        val byteArray = ByteArray(contentsAtPath.length.toInt())
        byteArray.usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
        return byteArray
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is MacOSResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
