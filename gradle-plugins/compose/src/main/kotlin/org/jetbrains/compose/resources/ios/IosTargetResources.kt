/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources.ios

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal abstract class IosTargetResources : Serializable {
    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val konanTarget: Property<String>

    @get:Input
    abstract val dirs: SetProperty<String>

    @Suppress("unused") // used by Gradle Configuration Cache
    fun readObject(input: ObjectInputStream) {
        name.set(input.readUTF())
        konanTarget.set(input.readUTF())
        dirs.set(input.readUTFStrings())
    }

    @Suppress("unused") // used by Gradle Configuration Cache
    fun writeObject(output: ObjectOutputStream) {
        output.writeUTF(name.get())
        output.writeUTF(konanTarget.get())
        output.writeUTFStrings(dirs.get())
    }

    private fun ObjectOutputStream.writeUTFStrings(collection: Collection<String>) {
        writeInt(collection.size)
        collection.forEach { writeUTF(it) }
    }

    private fun ObjectInputStream.readUTFStrings(): Set<String> {
        val size = readInt()
        return LinkedHashSet<String>(size).apply {
            repeat(size) {
                add(readUTF())
            }
        }
    }

    companion object {
        private const val serialVersionUID: Long = 0
    }
}