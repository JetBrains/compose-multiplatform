/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal data class JvmRuntimeProperties(
    val majorVersion: Int,
    val availableModules: List<String>,
) : Serializable {
    companion object {
        @Suppress("unused")
        private val serialVersionUid: Long = 0

        fun writeToFile(properties: JvmRuntimeProperties, file: File) {
            file.parentFile.mkdirs()
            file.delete()
            file.createNewFile()
            ObjectOutputStream(file.outputStream().buffered()).use { oos ->
                oos.writeObject(properties)
            }
        }

        fun readFromFile(file: File): JvmRuntimeProperties =
            ObjectInputStream(file.inputStream().buffered()).use { ois ->
                ois.readObject() as JvmRuntimeProperties
            }
    }
}