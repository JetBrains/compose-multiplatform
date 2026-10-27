/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.HtmlSymbols.Companion.InputScope
import org.jetbrains.compose.html.compiler.HtmlSymbols.Companion.TextAreaScope
import org.jetbrains.compose.html.compiler.ir.KotlinLet
import org.jetbrains.compose.html.compiler.ir.anyDescendant
import org.jetbrains.compose.html.compiler.ir.irCallWithArgs
import org.jetbrains.compose.html.compiler.ir.isAbsent
import org.jetbrains.compose.html.compiler.ir.unwrapReturn
import org.jetbrains.compose.html.compiler.ir.withOverrides
import org.jetbrains.compose.web.dom.AsciiWhitespaceCharacters
import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.escapeHtml
import org.jetbrains.compose.web.dom.isHtmlBooleanAttributeName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf

internal sealed interface HtmlAttributes

// Encoded attributes plus their original values for prepared-element rendering.
internal data class LiteralAttributes(
    val encoded: String,
    val values: Map<String, String>,
    val classNames: List<String>,
) : HtmlAttributes {
    fun openingTag(tag: String): String = "<$tag$encoded>"
}

internal enum class AttributeOperationKind { Attribute, Classes, Style, FormValue, FormChecked, TextContent, TextDefault, Ignore }

internal class AttributeOperation(
    val kind: AttributeOperationKind,
    val call: IrCall?,
    val name: String,
    val values: List<IrExpression>,
    var condition: IrExpression? = null,
    val valueIsAttribute: IrExpression? = null,
    var unconditional: Boolean = true,
)

// Keeps the attrs body and its operations for runtime evaluation in the original order.
internal class MixedAttributes(
    val body: IrBlockBody,
    val bodyOperations: List<AttributeOperation>,
    val lets: Map<IrCall, IrFunctionExpression>,
    val tag: String,
    val namespace: String,
    val before: List<AttributeOperation>,
    val after: List<AttributeOperation>,
) : HtmlAttributes {
    val operations: List<AttributeOperation> = before + bodyOperations + after
    val attributes = operations.filter { it.kind == AttributeOperationKind.Attribute }
    val classes = operations.filter { it.kind == AttributeOperationKind.Classes }
    val styles = operations.filter { it.kind == AttributeOperationKind.Style }
}

// Tries constant attributes first, then collects supported operations for runtime rendering.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlAttributeAnalyzer(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
    private val constants: ConstantEvaluator,
) {
    // Returns literal or mixed attributes, or null if the callback cannot be safely lowered.
    fun analyze(
        expression: IrExpression?,
        tag: String,
        before: List<AttributeOperation>,
        after: List<AttributeOperation>,
        namespace: String,
        valueIsAttribute: IrExpression?,
    ): HtmlAttributes? {
        if (tag.length.toLong() + 2 > MaxEncodedFragmentLength) return null
        val lambda = if (expression.isAbsent()) null else (expression as? IrFunctionExpression ?: return null)
        val receiver = lambda?.function?.parameters?.singleOrNull()?.symbol
        if (lambda != null && receiver == null) return null
        val body = if (lambda == null) context.irFactory.createBlockBody(0, 0) else lambda.function.body as? IrBlockBody ?: return null
        val analysis = Analysis(receiver, namespace, tag, valueIsAttribute)
        if (!body.statements.all { analysis.analyze(it, unconditional = true) }) return null
        val operations = before + analysis.operations + after
        if (operations.any {
                it.condition != null || when (it.kind) {
                    AttributeOperationKind.Attribute, AttributeOperationKind.Classes -> false
                    AttributeOperationKind.Ignore -> it.values.isNotEmpty()
                    else -> true
                }
            }) analysis.literal = false
        if (namespace != HtmlNamespace) {
            val names = operations.filter { it.kind != AttributeOperationKind.Classes }.map { it.name }.distinct()
            if (names.map { it.lowercase() }.distinct().size != names.size) return null
        }
        if (analysis.literal) {
            literalAttributes(operations, tag, namespace)
                ?.takeIf { tag.length.toLong() + it.encoded.length + 2 <= MaxEncodedFragmentLength }
                ?.let { return it }
        }
        if (operations.size + operations.sumOf { it.values.size } > MaxMixedAttributeOperations ||
            operations.filter { it.kind == AttributeOperationKind.Classes }.sumOf { it.values.size } > MaxMixedClassTokens) return null
        return MixedAttributes(body, analysis.operations.toList(), analysis.lets, tag, namespace, before, after)
    }

    private inner class Analysis(
        val receiver: IrValueSymbol?,
        val namespace: String,
        val tag: String,
        val valueIsAttribute: IrExpression?,
    ) {
        val operations = mutableListOf<AttributeOperation>()
        val lets = mutableMapOf<IrCall, IrFunctionExpression>()
        var literal = true // The operations may be encoded as HTML if their values are constant.

        private fun matches(actual: IrSimpleFunctionSymbol, expected: IrSimpleFunctionSymbol): Boolean =
            actual.withOverrides().any { it == expected }

        // The value must not use the attrs receiver or return from an enclosing function.
        // loadTitle() can pass this check. customHelper(this) cannot.
        private fun independent(element: IrElement): Boolean {
            val localFunctions = mutableSetOf<IrFunctionSymbol>()
            val returns = mutableSetOf<IrReturnTargetSymbol>()
            val capturesReceiver = element.anyDescendant {
                when (it) {
                    is IrFunction -> localFunctions.add(it.symbol)
                    is IrReturn -> returns.add(it.returnTargetSymbol)
                }
                it is IrGetValue && it.symbol == receiver
            }
            return !capturesReceiver && localFunctions.containsAll(returns)
        }

        // Collect operations from one attrs statement. False rejects the whole callback.
        fun analyze(statement: IrStatement, unconditional: Boolean = false): Boolean {
            if (constants.isConstantAlias(statement)) return true
            if (statement !is IrCall && (statement !is IrTypeOperatorCall || statement.argument !is IrCall)) literal = false
            return when (statement) {
                is IrTypeOperatorCall -> statement.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT &&
                        analyze(statement.argument, unconditional)

                is IrWhen -> statement.branches.all {
                    independent(it.condition) && analyze(it.result)
                }

                is IrContainerExpression -> statement.statements.all { analyze(it) }
                is IrCall -> analyzeCall(statement, unconditional)

                is IrLoop, is IrReturn -> false
                else -> independent(statement)
            }
        }

        private fun analyzeCall(call: IrCall, unconditional: Boolean): Boolean {
            if (call.symbol.owner.fqNameWhenAvailable == KotlinLet) return analyzeLet(call)
            if ((call.arguments.firstOrNull() as? IrGetValue)?.symbol != receiver) {
                literal = false
                return independent(call)
            }
            val operation = attributeOperation(call) ?: return false
            // Pure ignored callbacks and annotated helpers can join literal attributes.
            if (call.symbol != symbols.classes && call.symbol != symbols.id && call.symbol != symbols.attr &&
                symbols.attribute(call.symbol) == null &&
                !(operation.kind == AttributeOperationKind.Ignore && operation.values.isEmpty())) {
                literal = false
            }
            if (operation.values.any { !independent(it) }) return false
            operation.unconditional = unconditional
            operations.add(operation)
            return true
        }

        private fun analyzeLet(call: IrCall): Boolean {
            literal = false
            val value = call.arguments.getOrNull(0) ?: return false
            val function = call.arguments.getOrNull(1) as? IrFunctionExpression ?: return false
            val statements = (function.function.body as? IrBlockBody)?.statements ?: return false
            if (function.function.parameters.size != 1 || !independent(value)) return false
            if (!statements.all {
                    analyze(it.unwrapReturn(function.function.symbol))
                }) return false
            lets[call] = function
            return true
        }

        private fun attributeOperation(call: IrCall): AttributeOperation? {
            when {
                matches(call.symbol, symbols.classes) -> {
                    val vararg = call.arguments.getOrNull(1) as? IrVararg ?: return null
                    val values = vararg.elements.map { it as? IrExpression ?: return null }
                    return AttributeOperation(AttributeOperationKind.Classes, call, "", values)
                }

                matches(call.symbol, symbols.id) -> return AttributeOperation(
                    AttributeOperationKind.Attribute, call, "id", listOf(call.arguments.getOrNull(1) ?: return null)
                )

                matches(call.symbol, symbols.attr) -> {
                    val name = constants.string(call.arguments.getOrNull(1)) ?: return null
                    if (!isLowerableAttributeName(name, namespace)) return null
                    return AttributeOperation(
                        AttributeOperationKind.Attribute, call, name, listOf(call.arguments.getOrNull(2) ?: return null)
                    )
                }

                matches(call.symbol, symbols.style) -> return AttributeOperation(
                    AttributeOperationKind.Style, call, "", listOf(call.arguments.getOrNull(1) ?: return null)
                )

                isIgnored(call.symbol) -> return AttributeOperation(
                    AttributeOperationKind.Ignore, call, "", call.arguments.drop(1).filterNotNull().flatMap {
                        when (it) {
                            is IrFunctionExpression -> emptyList()
                            is IrFunctionReference -> it.arguments.filterNotNull()
                            else -> listOf(it)
                        }
                    }
                )
            }
            // Only specialized scope members and annotated helpers need an IR builder.
            val builder = DeclarationIrBuilder(context, call.symbol)
            return scopeOperation(call.symbol, call.arguments.getOrNull(1), call, tag, valueIsAttribute, builder)
                ?: helperOperation(call.symbol, call.arguments.getOrNull(1), call, builder)
        }
    }

    private fun stringValue(value: IrExpression, builder: DeclarationIrBuilder): IrExpression =
        if (value.type.isString()) value else builder.irCallWithArgs(symbols.anyToString, value)

    // Match a wrapper parameter to its attribute helper and convert the value if needed.
    fun wrapperOperation(
        name: String,
        value: IrExpression,
        namespace: String,
        builder: DeclarationIrBuilder,
        parameterType: IrType,
        receiver: IrType,
        tag: String,
        valueIsAttribute: IrExpression?,
    ): AttributeOperation? {
        val typeSystem = IrTypeSystemContextImpl(context.irBuiltIns)
        val helpers = symbols.helpers(name, namespace, receiver).filter { helper ->
            helper.owner.parameters.firstOrNull()?.let {
                it.kind == IrParameterKind.DispatchReceiver || receiver.isSubtypeOf(it.type, typeSystem)
            } == true
        }
        val type = parameterType.makeNotNull()
        val helper = helpers.firstOrNull { it.owner.parameters.size == 2 && it.owner.parameters[1].type.makeNotNull() == type }
            ?: helpers.firstOrNull { type.isBoolean() && it.owner.parameters.size == 1 }
            ?: helpers.firstOrNull { it.owner.parameters.size == 2 && it.owner.parameters[1].type.isString() }
            ?: return null
        val expected = helper.owner.parameters.getOrNull(1)?.type
        val converted = if (expected?.isString() == true && !type.isString()) stringValue(value, builder) else
            if (value.type.isNullable()) builder.irImplicitCast(value, type) else value
        return scopeOperation(helper, converted, null, tag, valueIsAttribute, builder)
            ?: helperOperation(helper, converted, null, builder)
    }

    private fun scopeOperation(
        symbol: IrSimpleFunctionSymbol,
        value: IrExpression?,
        call: IrCall?,
        tag: String,
        valueIsAttribute: IrExpression?,
        builder: DeclarationIrBuilder,
    ): AttributeOperation? {
        if (tag == "input" && isMember(symbol, InputScope, "value", "defaultValue", "checked", "defaultChecked")) {
            return when (symbol.owner.name.asString()) {
                "checked" -> AttributeOperation(AttributeOperationKind.FormChecked, call, "checked", listOf(value ?: return null))
                "defaultChecked" -> AttributeOperation(AttributeOperationKind.Attribute, call, "checked", emptyList())
                "defaultValue" -> AttributeOperation(
                    AttributeOperationKind.Attribute, call, "value", listOf(stringValue(value ?: return null, builder))
                )
                else -> AttributeOperation(
                    AttributeOperationKind.FormValue, call, "value", listOf(stringValue(value ?: return null, builder)),
                    valueIsAttribute = valueIsAttribute ?: return null
                )
            }
        }
        if (tag == "textarea" && isMember(symbol, TextAreaScope, "value", "defaultValue")) {
            val kind = if (symbol.owner.name.asString() == "value") {
                AttributeOperationKind.TextContent
            } else AttributeOperationKind.TextDefault
            return AttributeOperation(kind, call, "", listOf(value ?: return null))
        }
        return null
    }

    private fun helperOperation(
        symbol: IrSimpleFunctionSymbol,
        value: IrExpression?,
        call: IrCall?,
        builder: DeclarationIrBuilder,
    ): AttributeOperation? {
        val helper = symbols.attribute(symbol) ?: return null
        if (value == null && symbol.owner.parameters.size == 2 && helper.defaultValue != null) {
            return AttributeOperation(AttributeOperationKind.Attribute, call, helper.name, listOf(builder.irString(helper.defaultValue)))
        }
        val values = if (symbol.owner.parameters.size == 1) {
            if (helper.value.isEmpty()) emptyList() else listOf(builder.irString(helper.value))
        } else {
            val argument = value ?: return null
            val integer = if (helper.conversion == symbols.formatNumber) constants.integer(argument) else null
            listOf(when {
                integer != null -> builder.irString(integer.toString())
                helper.conversion != null -> builder.irCallWithArgs(helper.conversion, argument)
                else -> argument
            })
        }
        return AttributeOperation(AttributeOperationKind.Attribute, call, helper.name, values)
    }

    private fun isMember(symbol: IrSimpleFunctionSymbol, owner: String, vararg names: String): Boolean =
        symbol.withOverrides().any { it.owner.fqNameWhenAvailable?.asString() in names.map { "$owner.$it" } }

    private fun isIgnored(symbol: IrSimpleFunctionSymbol): Boolean = symbol.withOverrides().any {
        val fqName = it.owner.fqNameWhenAvailable?.asString().orEmpty()
        val name = it.owner.name.asString()
        it.owner.fqNameWhenAvailable in HtmlSymbols.FormEvents ||
            fqName.startsWith("${HtmlSymbols.EventsScope}.") ||
            (name.startsWith("on") && listOf(InputScope, TextAreaScope, HtmlSymbols.SelectScope).any { fqName == "$it.$name" }) ||
            fqName == "${HtmlSymbols.AttrsScope.asSingleFqName()}.ref"
    }

    /**
     * Encodes constant attributes and keeps their unescaped values for later use.
     *
     * ```kotlin
     * attrs = {
     *     id("profile")
     *     attr("title", "A&B")
     *     classes("card", "selected")
     * }
     * ```
     *
     * Produces:
     * ```html
     * id="profile" title="A&amp;B" class="card selected"
     * ```
     *
     * Returns `null` for dynamic values, NUL, duplicate names, invalid or duplicate class tokens,
     * or attributes that exceed the size limit. The caller can then try runtime attribute writes.
     */
    private fun literalAttributes(operations: List<AttributeOperation>, tagName: String, namespace: String): LiteralAttributes? {
        val classNames = mutableListOf<String>()
        val values = linkedMapOf<String, String>()

        for (operation in operations) {
            if (operation.kind == AttributeOperationKind.Ignore) continue
            if (operation.kind == AttributeOperationKind.Classes) {
                for (expression in operation.values) {
                    val value = constants.stringWithoutNul(expression) ?: return null
                    if (value.isEmpty() || value.any { it in InvalidClassTokenCharacters } || value in classNames) return null
                    classNames.add(value)
                }
            } else {
                val value = if (operation.values.isEmpty()) "" else constants.stringWithoutNul(operation.values.single()) ?: return null
                if (operation.name in values) return null
                values[operation.name] = value
            }
        }

        val encoded = buildString {
            for ((name, value) in values) {
                append(' ').append(name)
                if (value.isNotEmpty() || !isMinimizedBooleanAttribute(name, tagName, namespace)) {
                    append("=\"").append(escapeHtml(value, attribute = true)).append('"')
                }
                if (length > MaxEncodedFragmentLength) return null
            }
            // The runtime serializes ordinary attributes first and class tokens last.
            if (classNames.isNotEmpty() && "class" !in values) {
                append(" class=\"")
                append(escapeHtml(classNames.joinToString(" "), attribute = true)).append('"')
            }
            if (length > MaxEncodedFragmentLength) return null
        }
        return LiteralAttributes(encoded, values, classNames)
    }
}

private const val InvalidClassTokenCharacters = AsciiWhitespaceCharacters + '\u0000'

private val LowerableAttributeName = Regex("[a-z][a-z0-9_.:-]*")

// Keep reserved data-compose names and unsupported names on the runtime path.
internal fun isLowerableAttributeName(name: String, namespace: String): Boolean =
    (if (namespace == HtmlNamespace) LowerableAttributeName else ForeignAttributeName).matches(name) &&
        !name.startsWith("data-compose", ignoreCase = true)

private val ForeignAttributeName = Regex("[a-zA-Z][a-zA-Z0-9_.:-]*")

// Custom elements and foreign namespaces preserve empty boolean attribute values.
internal fun isMinimizedBooleanAttribute(name: String, tag: String, namespace: String): Boolean =
    namespace == HtmlNamespace && '-' !in tag && name.isHtmlBooleanAttributeName()
