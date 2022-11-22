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
    override suspend fun readBytes(): Result<ByteArray> {
        val currentDirectoryPath = NSFileManager.defaultManager().currentDirectoryPath
        val contentsAtPath: NSData? = NSFileManager.defaultManager().run {
            //todo in future bundle resources with app and use all sourceSets (skikoMain, nativeMain)
            contentsAtPath("$currentDirectoryPath/src/macosMain/resources/$path")
                ?: contentsAtPath("$currentDirectoryPath/src/commonMain/resources/$path")
        }
        if (contentsAtPath != null) {
            val byteArray = ByteArray(contentsAtPath.length.toInt())
            byteArray.usePinned {
                memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
            }
            return Result.success(byteArray)
        } else {
            return Result.failure(MissingResource(path))
        }
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
