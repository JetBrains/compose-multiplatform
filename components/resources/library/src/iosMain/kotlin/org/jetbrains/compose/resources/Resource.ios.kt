/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources


import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

@ExperimentalResourceApi
actual fun resource(path: String): Resource = UIKitResourceImpl(path)

@ExperimentalResourceApi
private class UIKitResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): LoadState<ByteArray> {
        val absolutePath = NSBundle.mainBundle.resourcePath + "/" + path
        val contentsAtPath: NSData = NSFileManager.defaultManager().contentsAtPath(absolutePath)!!
        val byteArray = ByteArray(contentsAtPath.length.toInt())
        byteArray.usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
        return LoadState.Success(byteArray)//todo fail case
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is UIKitResourceImpl) {
            path == other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
