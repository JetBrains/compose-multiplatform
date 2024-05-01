package com.example.jetsnack.model

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFUUIDCreate
import platform.CoreFoundation.CFUUIDCreateString

@OptIn(ExperimentalForeignApi::class)
actual fun createRandomUUID(): Long {
    val uuidRef = CFUUIDCreate(null)
    val uuidStringRef = CFUUIDCreateString(null, uuidRef) as String
    val uuidStr: String = uuidStringRef.replace("-", "")
    return uuidStr.substring(uuidStr.length - 16).toLong(16)
}
