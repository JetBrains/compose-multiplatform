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
private class MacOSResourceImpl(path: String) : AbstractResourceImpl(path) {
    override suspend fun readBytes(): ByteArray {
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
            return byteArray
        } else {
            throw MissingResourceException(path)
        }
    }
}

internal actual class MissingResourceException actual constructor(path: String) :
    Exception("Missing resource with path: $path")

