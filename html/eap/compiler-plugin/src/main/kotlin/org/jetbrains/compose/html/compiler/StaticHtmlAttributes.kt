/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.compose.web.dom.AsciiWhitespaceCharacters
import org.jetbrains.compose.web.dom.isHtmlBooleanAttributeName
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

private const val InvalidClassTokenCharacters = AsciiWhitespaceCharacters + '\u0000'

/** Encode a literal attribute lambda, or return null to keep its runtime evaluation. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class StaticHtmlAttributes(private val symbols: HtmlSymbols) {
    private val allowedName = Regex("[a-z][a-z0-9_.:-]*")

    /**
     * Handles literal `attrs` lambdas:
     * ```
     * ...(attrs = {
     *     classes("card", "selected")
     *     id("main")
     *     attr("title", "A & B")
     * }) { ... }
     * => '"id="main" title="A &amp; B" class="card selected"'
     * ```
     **/
    fun encode(expression: IrExpression?, tagName: String): String? {
        if (expression == null || (expression as? IrConst)?.kind == IrConstKind.Null) return ""
        val lambda = expression as? IrFunctionExpression ?: return null
        val receiver = lambda.function.parameters.singleOrNull()?.symbol ?: return null
        val body = lambda.function.body as? IrBlockBody ?: return null
        val classNames = mutableListOf<String>()
        val values = linkedMapOf<String, String>()

        for (statement in body.statements) {
            val call = when (statement) {
                is IrCall -> statement
                // attr returns AttrsScope, which Kotlin discards in a Unit-returning lambda.
                is IrTypeOperatorCall ->
                    statement.takeIf { it.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT }
                        ?.argument as? IrCall
                else -> null
            } ?: return null
            if ((call.arguments.firstOrNull() as? IrGetValue)?.symbol != receiver) return null

            if (call.symbol == symbols.classesSymbol) {
                val arguments = call.arguments.getOrNull(1) as? IrVararg ?: return null
                for (element in arguments.elements) {
                    val value = (element as? IrConst)?.value as? String ?: return null
                    if (value.isEmpty() || value.any { it in InvalidClassTokenCharacters }) return null
                    if (value in classNames) return null
                    classNames.add(value)
                }
            } else {
                val (name, valueArgument) = when (call.symbol) {
                    symbols.idSymbol -> "id" to call.arguments.getOrNull(1)
                    symbols.attrSymbol -> (literalString(call.arguments.getOrNull(1)) ?: return null) to
                        call.arguments.getOrNull(2)
                    else -> return null
                }
                val value = literalString(valueArgument) ?: return null
                if (!allowedName.matches(name) || name.startsWith("data-compose") || name in values) return null
                values[name] = value
            }
        }

        return buildString {
            for ((name, value) in values) {
                append(' ').append(name)
                // Custom elements preserve empty attribute values instead of minimizing booleans.
                if (value.isNotEmpty() || '-' in tagName || !name.isHtmlBooleanAttributeName()) {
                    append("=\"").append(escapeHtml(value, attribute = true)).append('"')
                }
            }
            // The runtime serializes ordinary attributes first and class tokens last.
            if (classNames.isNotEmpty() && "class" !in values) {
                append(" class=\"")
                append(escapeHtml(classNames.joinToString(" "), attribute = true)).append('"')
            }
        }
    }

    private fun literalString(expression: IrExpression?): String? =
        ((expression as? IrConst)?.value as? String)?.takeUnless { '\u0000' in it }
}
