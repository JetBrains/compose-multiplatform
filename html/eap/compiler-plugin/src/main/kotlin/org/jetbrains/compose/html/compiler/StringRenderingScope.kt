/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/** Finds content reachable from composeHtmlToString, including shared functions and stored lambdas. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun findStringRenderingFunctions(
    module: IrModuleFragment,
    composeHtmlToString: IrSimpleFunctionSymbol,
): Set<IrFunction> {
    val functions = mutableSetOf<IrFunction>()
    val fields = mutableSetOf<IrField>()
    val values = mutableMapOf<IrValueSymbol, MutableList<IrExpression>>()
    val fieldValues = mutableMapOf<IrField, MutableList<IrExpression>>()
    val calls = mutableListOf<IrCall>()

    fun bind(symbol: IrValueSymbol, value: IrExpression?) {
        if (value != null) values.getOrPut(symbol) { mutableListOf() }.add(value)
    }

    // Index declarations first so references can cross files or precede their definitions.
    module.acceptChildrenVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            when (element) {
                is IrFunction -> functions.add(element)
                is IrField -> {
                    fields.add(element)
                    element.initializer?.expression?.let {
                        fieldValues.getOrPut(element) { mutableListOf() }.add(it)
                    }
                }
                is IrVariable -> bind(element.symbol, element.initializer)
                is IrSetValue -> bind(element.symbol, element.value)
                is IrSetField -> fieldValues.getOrPut(element.symbol.owner) { mutableListOf() }.add(element.value)
                is IrCall -> calls.add(element)
            }
            element.acceptChildrenVoid(this)
        }
    })

    // Follow content forwarded through parameters. Shared parameters may have several callers.
    for (call in calls) {
        val function = call.symbol.owner
        if (function !in functions) continue
        for ((index, parameter) in function.parameters.withIndex()) {
            bind(parameter.symbol, call.arguments[index] ?: parameter.defaultValue?.expression)
        }
    }

    val pending = ArrayDeque<IrElement>()
    val visited = mutableSetOf<IrElement>()
    val reachable = mutableSetOf<IrFunction>()
    fun enqueue(element: IrElement?) {
        if (element != null && visited.add(element)) pending.addLast(element)
    }
    val children = object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) = enqueue(element)
    }

    val contentIndex = composeHtmlToString.owner.parameters.indexOfFirst { it.name.asString() == "content" }
    for (call in calls) {
        if (call.symbol == composeHtmlToString) enqueue(call.arguments[contentIndex])
    }

    while (pending.isNotEmpty()) {
        val element = pending.removeFirst()
        when (element) {
            is IrFunction -> {
                // Dependencies have already been compiled; only change this compilation's declarations.
                if (element !in functions) continue
                reachable.add(element)
            }
            is IrCall -> enqueue(element.symbol.owner.takeIf { it in functions })
            is IrFunctionReference -> enqueue(element.symbol.owner.takeIf { it in functions })
            is IrGetValue -> values[element.symbol]?.forEach(::enqueue)
            is IrGetField -> if (element.symbol.owner in fields) {
                fieldValues[element.symbol.owner]?.forEach(::enqueue)
            }
        }
        element.acceptChildrenVoid(children)
    }
    return reachable
}
