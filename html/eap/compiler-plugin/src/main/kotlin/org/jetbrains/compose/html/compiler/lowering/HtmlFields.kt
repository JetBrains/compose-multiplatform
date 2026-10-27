/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

// Creates private fields with unique names and shares large strings within one file.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlFields(private val context: IrPluginContext, private val file: IrFile) {
    private val usedNames =
        file.declarations.filterIsInstance<IrDeclarationWithName>().mapTo(mutableSetOf()) { it.name }
    private val shared = mutableMapOf<String, IrField>()
    private var length = 0
    val stringFields: Collection<IrField> get() = shared.values

    // Count only new large strings against the file's cache limit.
    fun canStore(values: Collection<String>): Boolean =
        length.toLong() + values.toSet().sumOf {
            if (it.length > MaxHtmlChunkLength && it !in shared) it.length.toLong() else 0L
        } <= MaxCachedStringLengthPerFile

    // Use a literal for small strings and a shared field for large ones.
    fun string(value: String, builder: DeclarationIrBuilder): IrExpression {
        if (value.length <= MaxHtmlChunkLength) return builder.irString(value)
        val field = shared.getOrPut(value) {
            check(canStore(listOf(value)))
            length += value.length
            create("__htmlString", shared.size, context.irBuiltIns.stringType).apply {
                // A plain oversized literal lets the backend split the constant.
                initializer = context.irFactory.createExpressionBody(builder.irString(value))
            }
        }
        return builder.irGetField(null, field)
    }

    fun create(
        prefix: String,
        index: Int,
        type: IrType,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrField = context.irFactory.buildField {
        name = generateSequence(index) { it + 1 }.map { Name.identifier("$prefix$it") }.first(usedNames::add)
        this.type = type
        visibility = DescriptorVisibilities.PRIVATE
        isFinal = true
        isStatic = true
        this.startOffset = startOffset
        this.endOffset = endOffset
    }.apply { parent = file }
}
