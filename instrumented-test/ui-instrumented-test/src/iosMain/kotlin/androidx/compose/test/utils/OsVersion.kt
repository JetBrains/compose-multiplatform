/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalForeignApi::class)
internal fun available(iosMajorVersion: Int, iosMinorVersion: Int = 0): Boolean {
    return NSProcessInfo.processInfo.operatingSystemVersion.useContents {
        when {
            majorVersion.toInt() < iosMajorVersion -> false
            majorVersion.toInt() > iosMajorVersion -> true
            minorVersion.toInt() < iosMinorVersion -> false
            else -> true
        }
    }
}