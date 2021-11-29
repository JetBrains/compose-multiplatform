/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

data class Command(val type: Type, val args: List<String>) {
    enum class Type {
        ATTACH,
        FRAME,
        ERROR,
        PREVIEW_CONFIG,
        PREVIEW_CLASSPATH,
        PREVIEW_FQ_NAME,
        FRAME_REQUEST
    }

    constructor(type: Type, vararg args: String) : this(type, args.toList())

    fun asString() =
        (sequenceOf(type.name) + args.asSequence()).joinToString(" ")

    companion object {
        private val typeByName: Map<String, Type> =
            Type.values().associateBy { it.name }

        fun fromString(s: String): Command? {
            val wordsIt = s.splitToSequence(" ").iterator()
            val cmdName = wordsIt.nextOrNull() ?: return null
            val type = typeByName[cmdName] ?: return null
            val args = arrayListOf<String>()
            wordsIt.forEachRemaining {
                args.add(it)
            }
            return Command(type, args)
        }
    }
}
