/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Assigns deterministic, inert JVM values to numeric companion constants.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName

/**
 * Allocates a stable value for each distinct `(type, name)` in one emitted model.
 *
 * Sorting makes allocation independent of KSP traversal order. Sharing the key across owners keeps
 * inherited constants consistent, while using a separate namespace for each numeric type keeps
 * every generated literal type-correct.
 */
internal class JvmConstantValues(constants: Iterable<PortableConstant>) {
    private data class Key(val type: TypeName, val name: String)

    private val values: Map<Key, Int> = constants
        .groupBy(PortableConstant::type)
        .flatMap { (type, constantsOfType) ->
            val names = constantsOfType.map(PortableConstant::name).distinct().sorted()
            val capacity = type.nonNegativeConstantCapacity()
            check(names.size.toLong() <= capacity) {
                "Cannot assign ${names.size} distinct JVM companion constants to $type; " +
                    "only $capacity non-negative values fit"
            }
            names.mapIndexed { value, name -> Key(type, name) to value }
        }
        .toMap()

    fun initializer(constant: PortableConstant): CodeBlock {
        val value = checkNotNull(values[Key(constant.type, constant.name)]) {
            "No JVM companion constant allocation for ${constant.name}: ${constant.type}"
        }
        return CodeBlock.of(if (constant.type == LONG) "%LL" else "%L", value)
    }
}

private fun TypeName.nonNegativeConstantCapacity(): Long = when (this) {
    BYTE -> Byte.MAX_VALUE.toLong() + 1
    SHORT -> Short.MAX_VALUE.toLong() + 1
    INT -> Int.MAX_VALUE.toLong() + 1
    // An in-memory model cannot contain enough names to approach this limit.
    LONG -> Long.MAX_VALUE
    else -> error("Unsupported JVM companion constant type: $this")
}
