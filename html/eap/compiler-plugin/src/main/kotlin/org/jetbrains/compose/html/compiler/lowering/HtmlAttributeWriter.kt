/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.irCallWithArgs
import org.jetbrains.compose.html.compiler.ir.unwrapReturn
import org.jetbrains.compose.web.dom.escapeHtml
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irEqualsNull
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

private val AttributeSlotKinds = setOf(
    AttributeOperationKind.Attribute, AttributeOperationKind.FormValue, AttributeOperationKind.FormChecked
)

private val PropertySlotKinds = setOf(
    AttributeOperationKind.FormValue, AttributeOperationKind.FormChecked,
    AttributeOperationKind.TextContent, AttributeOperationKind.TextDefault
)

/**
 * Writes analyzed attributes directly, without creating an attribute builder.
 *
 * ```kotlin
 * Div(attrs = {
 *     attr("title", loadTitle())
 * }) =>
 * val title = loadTitle()
 * writer.writeMixedLiteral("<div")                    // Writes <div
 * writer.writeMixedAttribute(" title", title, false)  // Writes title="..." with an escaped value.
 * writer.finishMixedElement("div")                   // Writes > and updates the parent state.
 * ```
 *
 * Values are evaluated once in their original order, then escaped when written.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlAttributeWriter(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
    private val constants: ConstantEvaluator,
    private val fields: HtmlFields,
) {
    private val unit = context.irBuiltIns.unitType
    private val nullableString = context.irBuiltIns.stringType.makeNullable()

    fun canEmit(plan: MixedAttributes): Boolean =
        fields.canStore(plan.operations.filter {
            it.kind in AttributeSlotKinds
        }.map { " ${it.name}" } + "<${plan.tag}")

    // Evaluate attributes in their original order, then write the opening tag.
    // Returns the parent state needed to close the element.
    fun emit(
        plan: MixedAttributes,
        builder: DeclarationIrBuilder,
        writer: IrVariable,
        validate: IrVariable,
    ): IrExpression = builder.irBlock(resultType = symbols.finishMixedElement.owner.returnType) {
        val state = assignSlots(plan)
        for (operation in plan.before) +operation(operation, operation.values, state, builder)
        for (statement in rewriteBody(plan, state, builder)) +statement
        for (operation in plan.after) +operation(operation, operation.values, state, builder)
        state.properties[AttributeOperationKind.FormValue]?.let { slot ->
            +irIfThen(
                unit, irNotEquals(irGet(slot), irNull()),
                assign("value", listOf(irGet(slot)), state, builder)
            )
        }
        state.properties[AttributeOperationKind.FormChecked]?.let { slot ->
            +irIfThen(
                unit, irNotEquals(irGet(slot), irNull()),
                irIfThenElse(
                    unit,
                    irEquals(irGet(slot), irTrue()),
                    assign("checked", emptyList(), state, builder),
                    irSet(state.attributes.getValue("checked"), irNull())
                )
            )
        }
        validateClasses(state, writer, validate)
        writeAttributes(plan, state, builder, writer)
        writeClasses(state, builder, writer)
        state.style?.let { slot ->
            val outputStyle = irCallWithArgs(symbols.writeMixedStyle, irGet(writer), irGet(slot))
            val explicit = state.attributes["style"]
            +if (explicit == null) outputStyle else irIfThen(
                unit, irEqualsNull(irGet(explicit)), outputStyle
            )
        }
        val parent = irTemporary(
            irCallWithArgs(symbols.finishMixedElement, irGet(writer), irString(plan.tag), irString(plan.namespace)),
            "htmlParent"
        )
        if (plan.tag == "textarea") {
            val text = state.properties[AttributeOperationKind.TextContent]
            val default = state.properties[AttributeOperationKind.TextDefault]
            if (text != null || default != null) {
                val value = irTemporary(
                    when {
                        text == null -> irGet(requireNotNull(default))
                        default == null -> irGet(text)
                        else -> irIfThenElse(nullableString, irEqualsNull(irGet(text)), irGet(default), irGet(text))
                    }, "htmlTextContent"
                )
                +irIfThen(
                    unit, irNotEquals(irGet(value), irNull()),
                    irCallWithArgs(
                        symbols.writeText,
                        irGet(writer),
                        irImplicitCast(irGet(value), context.irBuiltIns.stringType),
                        irGet(validate)
                    )
                )
            }
        }
        +irGet(parent)
    }

    private class Slots(
        val names: List<String>,
        val attributes: Map<String, IrVariable>,
        val order: Map<String, IrVariable>,
        val count: IrVariable?,
        val classes: Map<AttributeOperation, List<IrVariable>>,
        val properties: Map<AttributeOperationKind, IrVariable>,
        val style: IrVariable?,
    )

    // Keep attribute values in locals. Repeated names also track their first insertion order.
    private fun IrBlockBuilder.assignSlots(plan: MixedAttributes): Slots {
        val names = plan.operations.filter {
            it.kind in AttributeSlotKinds
        }.map { it.name }.distinct()
        val slots = names.associateWith { name ->
            irTemporary(
                irNull(nullableString),
                "html_${name.replace('-', '_')}",
                isMutable = true
            )
        }
        val repeated = plan.attributes.size != names.size || plan.operations.any {
            it.kind == AttributeOperationKind.FormValue || it.kind == AttributeOperationKind.FormChecked
        }
        val order = if (repeated) names.associateWith {
            irTemporary(irInt(-1), "htmlAttributeOrder", isMutable = true)
        } else emptyMap()
        val count = if (repeated) irTemporary(irInt(0), "htmlAttributeCount", isMutable = true) else null
        val classSlots = plan.classes.associateWith { operation ->
            operation.values.map {
                irTemporary(
                    irNull(nullableString),
                    "htmlClass",
                    isMutable = true
                )
            }
        }
        val properties = plan.operations.map { it.kind }.filter {
            it in PropertySlotKinds
        }.distinct().associateWith { kind ->
            val type =
                if (kind == AttributeOperationKind.FormChecked) context.irBuiltIns.booleanType else context.irBuiltIns.stringType
            irTemporary(irNull(type.makeNullable()), "html${kind.name}", isMutable = true)
        }
        val style = if (plan.styles.isEmpty()) null else irTemporary(
            irNull(symbols.mixedStyle.owner.returnType.makeNullable()), "htmlStyle", isMutable = true
        )
        return Slots(names, slots, order, count, classSlots, properties, style)
    }

    private fun assign(
        name: String,
        values: List<IrExpression>,
        state: Slots,
        builder: DeclarationIrBuilder
    ): IrExpression =
        builder.irBlock(resultType = unit) {
            val slot = state.attributes.getValue(name)
            val value = irTemporary(values.singleOrNull() ?: irString(""), "htmlValue")
            if (state.count != null) {
                +irIfThen(
                    unit, irEqualsNull(irGet(slot)),
                    irBlock(resultType = unit) {
                        +irSet(state.order.getValue(name), irGet(state.count))
                        +irSet(
                            state.count,
                            irCallWithArgs(context.irBuiltIns.intPlusSymbol, irGet(state.count), irInt(1))
                        )
                    })
            }
            +irSet(slot, irGet(value))
        }

    private fun operation(
        operation: AttributeOperation,
        values: List<IrExpression>,
        state: Slots,
        builder: DeclarationIrBuilder,
    ): IrExpression {
        val result = builder.irBlock(resultType = unit) {
            when (operation.kind) {
                AttributeOperationKind.Classes -> for ((slot, value) in state.classes.getValue(operation)
                    .zip(values)) +irSet(slot, value)

                AttributeOperationKind.Attribute -> +assign(operation.name, values, state, builder)
                AttributeOperationKind.Style -> {
                    val slot = requireNotNull(state.style)
                    +irSet(slot, irCallWithArgs(symbols.mixedStyle, irGet(slot), values.single()))
                }

                AttributeOperationKind.FormValue -> {
                    val value = irTemporary(values.single(), "htmlFormValue")
                    val condition = requireNotNull(operation.valueIsAttribute).deepCopyWithSymbols()
                    val attribute = (condition as? IrConst)?.value as? Boolean
                    val setAttribute = assign("value", listOf(irGet(value)), state, builder)
                    val setProperty = irSet(state.properties.getValue(AttributeOperationKind.FormValue), irGet(value))
                    +when (attribute) {
                        true -> setAttribute
                        false -> setProperty
                        null -> irIfThenElse(
                            unit,
                            condition, setAttribute, setProperty
                        )
                    }
                }

                AttributeOperationKind.Ignore -> for (value in values) +value
                else -> +irSet(state.properties.getValue(operation.kind), values.single())
            }
        }
        return operation.condition?.let { builder.irIfThen(unit, it.deepCopyWithSymbols(), result) } ?: result
    }

    // Replace attrs calls with local assignments, keeping branches and supported let bodies.
    private fun rewriteBody(plan: MixedAttributes, state: Slots, builder: DeclarationIrBuilder): List<IrStatement> {
        val byCall = plan.bodyOperations.associateBy { requireNotNull(it.call) }
        val statementExpressions = mutableSetOf<IrExpression>()
        fun recordStatements(statement: IrStatement) {
            when (statement) {
                is IrContainerExpression -> {
                    statementExpressions.add(statement)
                    statement.statements.forEach(::recordStatements)
                }

                is IrWhen -> {
                    statementExpressions.add(statement)
                    statement.branches.forEach { recordStatements(it.result) }
                }

                is IrTypeOperatorCall -> recordStatements(statement.argument)
                is IrCall -> plan.lets[statement]?.function?.body?.let { (it as IrBlockBody).statements.forEach(::recordStatements) }
                is IrReturn -> recordStatements(statement.value)
            }
        }
        plan.body.statements.forEach(::recordStatements)
        val replacement = object : IrElementTransformerVoid() {
            val aliases = mutableMapOf<IrValueSymbol, IrVariable>()

            override fun visitGetValue(expression: IrGetValue): IrExpression =
                aliases[expression.symbol]?.let { builder.irGet(it) } ?: expression

            override fun visitWhen(expression: IrWhen): IrExpression {
                val visited = super.visitWhen(expression) as IrWhen
                // Attribute statements discard their result. Value expressions are visited separately.
                if (expression in statementExpressions) visited.type = unit
                return visited
            }

            override fun visitBlock(expression: IrBlock): IrExpression {
                val visited = super.visitBlock(expression) as IrBlock
                if (expression in statementExpressions) visited.type = unit
                return visited
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val function = plan.lets[expression]?.function
                val transformer = this
                if (function != null) return builder.irBlock(resultType = unit) {
                    val parameter = function.parameters.single().symbol
                    val value = irTemporary(
                        requireNotNull(expression.arguments[0]).transform(transformer, null),
                        "htmlLetValue"
                    )
                    aliases[parameter] = value
                    for (statement in (function.body as IrBlockBody).statements) {
                        val result = statement.unwrapReturn(function.symbol)
                        +(result.transform(transformer, null) as IrStatement)
                    }
                    aliases.remove(parameter)
                }
                val original = byCall[expression] ?: return super.visitCall(expression)
                val values = original.values.map { it.transform(this, null) }
                return operation(original, values, state, builder)
            }
        }
        return plan.body.statements.map { it.transform(replacement, null) as IrStatement }
    }

    private fun IrBlockBuilder.validateClasses(state: Slots, writer: IrVariable, validate: IrVariable) {
        val classes = state.classes.values.flatten()
        if (classes.isNotEmpty()) +irIfThen(
            unit, irGet(validate),
            irBlock(resultType = unit) {
                for ((index, slot) in classes.withIndex()) {
                    var duplicate: IrExpression = irFalse()
                    for (previous in classes.take(index)) {
                        duplicate = irIfThenElse(
                            context.irBuiltIns.booleanType,
                            irEquals(irGet(slot), irGet(previous)), irTrue(), duplicate
                        )
                    }
                    +irCallWithArgs(symbols.validateMixedClass, irGet(writer), irGet(slot), irGet(validate), duplicate)
                }
            })
    }

    private fun IrBlockBuilder.writeAttributes(
        plan: MixedAttributes, state: Slots, builder: DeclarationIrBuilder, writer: IrVariable,
    ) {
        val tag = plan.tag
        val singleOperations = plan.attributes.groupBy { it.name }
        fun literal(operation: AttributeOperation): String? {
            val value = if (operation.values.isEmpty()) "" else
                constants.stringWithoutNul(operation.values.single()) ?: return null
            val boolean = isMinimizedBooleanAttribute(operation.name, tag, plan.namespace)
            return " ${operation.name}" + if (boolean && value.isEmpty()) "" else "=\"${
                escapeHtml(
                    value,
                    attribute = true
                )
            }\""
        }

        fun fixedLiteral(name: String): String? = singleOperations[name]?.singleOrNull()
            ?.takeIf { it.unconditional && it.condition == null }?.let(::literal)

        fun attribute(name: String): IrExpression {
            val controlled = (name == "value" && AttributeOperationKind.FormValue in state.properties) ||
                    (name == "checked" && AttributeOperationKind.FormChecked in state.properties)
            val single = if (controlled) null else singleOperations[name]?.singleOrNull()
            val encoded = single?.let(::literal)
            val boolean = isMinimizedBooleanAttribute(name, tag, plan.namespace)
            if (encoded != null && fields.canStore(listOf(encoded))) return irIfThen(
                unit,
                irNotEquals(irGet(state.attributes.getValue(name)), irNull()),
                irCallWithArgs(symbols.writeMixedLiteral, irGet(writer), fields.string(encoded, builder))
            )
            return irCallWithArgs(
                symbols.writeMixedAttribute,
                irGet(writer),
                fields.string(" $name", builder),
                irGet(state.attributes.getValue(name)),
                irBoolean(boolean)
            )
        }

        val opening = StringBuilder("<$tag")
        var fixed = 0
        if (state.count == null) for (name in state.names) {
            val literal = fixedLiteral(name) ?: break
            if (opening.length.toLong() + literal.length > MaxHtmlChunkLength) break
            opening.append(literal)
            fixed++
        }
        +irCallWithArgs(symbols.writeMixedLiteral, irGet(writer), fields.string(opening.toString(), builder))
        if (state.count == null) {
            for (name in state.names.drop(fixed)) +attribute(name)
        } else {
            val index = irTemporary(irInt(0), "htmlAttributeIndex", isMutable = true)
            +irWhile().apply {
                condition = irNotEquals(irGet(index), irGet(state.count))
                this.body = irBlock(resultType = unit) {
                    for (name in state.names) {
                        +irIfThen(
                            unit,
                            irEquals(irGet(index), irGet(state.order.getValue(name))), attribute(name)
                        )
                    }
                    +irSet(index, irCallWithArgs(context.irBuiltIns.intPlusSymbol, irGet(index), irInt(1)))
                }
            }
        }
    }

    private fun IrBlockBuilder.writeClasses(state: Slots, builder: DeclarationIrBuilder, writer: IrVariable) {
        if (state.classes.isNotEmpty()) {
            val outputClasses = irBlock(resultType = unit) {
                val written = irTemporary(irFalse(), "htmlClassesWritten", isMutable = true)
                for ((operation, slots) in state.classes) {
                    for ((slot, value) in slots.zip(operation.values)) {
                        val literal = constants.stringWithoutNul(value)
                            ?.let { escapeHtml(it, attribute = true) }
                            ?.takeIf { fields.canStore(listOf(it)) }
                        val value = if (literal == null) irGet(slot) else irIfThenElse(
                            nullableString, irEqualsNull(irGet(slot)), irNull(), fields.string(literal, builder)
                        )
                        +irSet(
                            written, irCallWithArgs(
                                symbols.writeMixedClass,
                                irGet(writer),
                                value,
                                irGet(written),
                                irBoolean(literal != null)
                            )
                        )
                    }
                }
                +irCallWithArgs(symbols.finishMixedClasses, irGet(writer), irGet(written))
            }
            val explicit = state.attributes["class"]
            +if (explicit == null) outputClasses else irIfThen(
                unit, irEqualsNull(irGet(explicit)), outputClasses
            )
        }
    }
}
