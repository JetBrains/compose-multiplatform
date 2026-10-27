/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import java.util.IdentityHashMap
import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.irCallWithArgs
import org.jetbrains.compose.html.compiler.ir.irLambda
import org.jetbrains.compose.html.compiler.ir.isAbsent
import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.isValidHtmlTagName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.Name

// A recognized HTML call with its tag, namespace, analyzed attributes and content.
internal class HtmlElement(
    val call: IrCall,
    val tag: String,
    val attributes: HtmlAttributes,
    val namespace: String,
    val content: IrExpression?,
) {
    val openingTag: String?
        get() = (attributes as? LiteralAttributes)?.openingTag(tag)
}

/**
 * Collects the tag, attributes and content of supported HTML calls.
 * Dynamic arguments are stored in temporary variables so they run once.
 * The original call remains available if later lowering cannot replace it.
 *
 * ```kotlin
 * A(href = computeUrl(), attrs = { attr("title", "Details") }) {
 *     Text("Open")
 * }
 * ```
 *
 * This gives an `a` element with `href` and `title` attributes and a `Text("Open")` body.
 * `computeUrl()` still runs at runtime.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class WrapperCallNormalizer(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
    private val constants: ConstantEvaluator,
    private val analyzer: HtmlAttributeAnalyzer,
) : IrElementTransformerVoidWithContext() {
    val elements = IdentityHashMap<IrCall, HtmlElement>()

    private class TagSpec(marker: IrAnnotation?) {
        private val arguments = marker?.argumentMapping.orEmpty()
        private fun string(name: String) =
            (arguments[Name.identifier(name)] as? IrConst)?.value as? String ?: ""

        private fun strings(name: String) = (arguments[Name.identifier(name)] as? IrVararg)?.elements
            ?.mapNotNull { (it as? IrConst)?.value as? String }.orEmpty()

        val tagName = string("tagName")
        val namespace = string("namespace").ifEmpty { HtmlNamespace }
        val text = string("text")
        val inputType = string("inputType")
        val omitEmpty = strings("omitEmpty").toSet()
        val afterAttrs = strings("afterAttrs").toSet()
        val defaults = strings("defaults").associate {
            it.substringBefore('=') to it.takeIf { '=' in it }?.substringAfter('=')
        }
    }

    private class Wrapper(
        val call: IrCall,
        val spec: TagSpec,
        val tag: String,
        val namespace: String,
        val attrs: IrValueParameter?,
        val text: IrValueParameter?,
        val content: IrValueParameter?,
        val inputType: IrValueParameter?,
        val positional: List<IrValueParameter>,
    ) {
        val hoisted = (positional + listOfNotNull(text, inputType)).sortedBy { it.indexInParameters }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as IrCall
        val wrapper = recognize(call) ?: return call
        val builder = DeclarationIrBuilder(
            context, requireNotNull(currentScope).scope.scopeOwnerSymbol, call.startOffset, call.endOffset
        )
        val defaults = defaults(wrapper, builder) ?: return call
        return builder.irBlock(resultType = call.type) {
            val temporaries = hoistArguments(wrapper, defaults)
            val attributes = wrapperAttributes(wrapper, temporaries, builder) ?: return call
            val content = wrapper.text?.let { textContent(temporaries.getValue(it), builder) }
                ?: call.argument(wrapper.content)
            elements[call] = HtmlElement(call, wrapper.tag, attributes, wrapper.namespace, content)
            // A budget fallback still calls the untouched wrapper with already evaluated arguments.
            for ((parameter, value) in temporaries) {
                if (call.arguments[parameter] != null) call.arguments[parameter] = value.deepCopyWithSymbols()
            }
            +call
        }.let { block ->
            block.patchDeclarationParents(builder.scope.scopeOwnerSymbol.owner as IrDeclarationParent)
            if (block.statements.size == 1) block.statements.single() as IrExpression else block
        }
    }

    // Recognize marked wrappers, Input and direct TagElement calls with supported arguments.
    private fun recognize(call: IrCall): Wrapper? {
        val function = call.symbol.owner
        val marker = function.annotations.singleOrNull { it.classSymbol == symbols.tagMarker }
        val input = call.symbol in symbols.inputs
        val direct = function.fqNameWhenAvailable in HtmlSymbols.DirectTags

        if (marker == null && !input && !direct) return null
        val receiver = function.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }
        if (receiver != null && call.arguments[receiver] !is IrGetValue) return null
        val attrs = function.parameter(if (direct) "applyAttrs" else "attrs")
        if (attrs == null && !input) return null
        val attrsArgument = call.argument(attrs)
        if (!attrsArgument.isAbsent() && attrsArgument !is IrFunctionExpression) return null

        val spec = TagSpec(marker)
        val tag = when {
            direct -> directTag(call) ?: return null
            input -> "input"
            else -> spec.tagName.ifEmpty { function.name.asString().lowercase() }
        }
        val namespaceParameter = function.parameter("namespace").takeIf { direct }
        val namespace =
            if (namespaceParameter != null) constants.string(call.arguments[namespaceParameter]) ?: return null
            else spec.namespace
        // foreignObject switches its children from SVG to HTML; leave that boundary to runtime rendering.
        if (!isValidHtmlTagName(tag) || tag == "foreignObject") return null
        val text = spec.text.takeIf { it.isNotEmpty() }?.let(function::parameter)
        if (text != null && call.arguments[text] == null) return null
        val content = function.parameter("content").takeIf { it != text }
        val inputType = function.parameter("type").takeIf { input }
        val positional = if (direct || input) emptyList() else function.parameters.filter {
            it.kind == IrParameterKind.Regular && it != attrs && it != content && it != text
        }

        return Wrapper(call, spec, tag, namespace, attrs, text, content, inputType, positional)
    }

    private fun directTag(call: IrCall): String? {
        val function = call.symbol.owner
        function.parameter("tagName")?.let { return constants.string(call.arguments[it]) }
        val factory = call.argument(function.parameter("elementBuilder")) as? IrCall ?: return null
        if (factory.symbol.owner.fqNameWhenAvailable != HtmlSymbols.CreateBuilder ||
            factory.symbol.owner.parameters.count { it.kind == IrParameterKind.Regular } != 1
        ) return null

        return constants.string(factory.arguments.lastOrNull())
    }

    // Fill omitted arguments from marker metadata or null. Leave unsupported defaults at runtime.
    private fun defaults(wrapper: Wrapper, builder: DeclarationIrBuilder): Map<IrValueParameter, IrExpression>? {
        val defaults = mutableMapOf<IrValueParameter, IrExpression>()
        for (parameter in wrapper.hoisted) {
            if (wrapper.call.arguments[parameter] != null) continue
            val value = wrapper.spec.defaults[parameter.name.asString()]
            defaults[parameter] = if (value != null) {
                when {
                    parameter.type.isString() -> builder.irString(value)
                    parameter.type.isBoolean() -> builder.irBoolean(value.toBooleanStrictOrNull() ?: return null)
                    value.toIntOrNull() != null -> builder.irInt(value.toInt())
                    else -> return null
                }
            } else if (parameter.type.isNullable()) builder.irNull(parameter.type) else return null
        }
        return defaults
    }

    private fun IrBlockBuilder.hoistArguments(
        wrapper: Wrapper,
        defaults: Map<IrValueParameter, IrExpression>,
    ): Map<IrValueParameter, IrExpression> = wrapper.hoisted.associateWith { parameter ->
        val value = wrapper.call.arguments[parameter] ?: defaults.getValue(parameter)
        if (value is IrConst || constants.string(value) != null || constants.integer(value) != null) value
        else irGet(irTemporary(value, "html_${parameter.name}"))
    }

    // Merge wrapper parameters with the attrs body in the order defined by the wrapper metadata.
    private fun wrapperAttributes(
        wrapper: Wrapper,
        values: Map<IrValueParameter, IrExpression>,
        builder: DeclarationIrBuilder,
    ): HtmlAttributes? = with(builder) {
        val before = mutableListOf<AttributeOperation>()
        val after = mutableListOf<AttributeOperation>()
        val typeName = wrapper.spec.inputType.takeIf { it.isNotEmpty() }
        val typeValue = wrapper.inputType?.let { values.getValue(it) }
        val inputType = typeValue?.let { irCallWithArgs(symbols.inputTypeValue, it) }
            ?: typeName?.let(::irString)
        if (inputType != null) before.add(
            AttributeOperation(
                AttributeOperationKind.Attribute,
                null,
                "type",
                listOf(inputType)
            )
        )
        val attrsType = wrapper.attrs?.type?.makeNotNull() as? IrSimpleType
        val attrsReceiver = attrsType?.arguments?.firstOrNull()?.typeOrNull
        val valueIsAttribute =
            typeValue?.let { irCallWithArgs(symbols.inputValueIsAttribute, it.deepCopyWithSymbols()) }
                ?: typeName?.let { irBoolean(it in HtmlSymbols.ValueAttributeInputTypes) }
        for (parameter in wrapper.positional) {
            val name = parameter.name.asString()
            val value = values.getValue(parameter)
            val operation = analyzer.wrapperOperation(
                name = name,
                value = value,
                namespace = wrapper.namespace,
                builder = builder,
                parameterType = parameter.type,
                receiver = attrsReceiver ?: return null,
                tag = wrapper.tag,
                valueIsAttribute = valueIsAttribute,
            ) ?: return null
            if (value.isAbsent()) continue

            val condition = when {
                name in wrapper.spec.omitEmpty -> when (constants.string(value)) {
                    "" -> continue
                    null -> irNotEquals(value.deepCopyWithSymbols(), irString(""))
                    else -> null
                }

                parameter.type.isBoolean() && operation.values.isEmpty() -> when ((value as? IrConst)?.value) {
                    false -> continue
                    true -> null
                    else -> value.deepCopyWithSymbols()
                }

                else -> null
            }
            val present = if (value.type.isNullable()) irNotEquals(value.deepCopyWithSymbols(), irNull()) else null
            operation.condition = if (present != null && condition != null) {
                irIfThenElse(context.irBuiltIns.booleanType, present, condition, irFalse())
            } else present ?: condition
            (if (name in wrapper.spec.afterAttrs) after else before).add(operation)
        }
        analyzer.analyze(
            expression = wrapper.call.argument(wrapper.attrs),
            tag = wrapper.tag,
            before = before,
            after = after,
            namespace = wrapper.namespace,
            valueIsAttribute = valueIsAttribute,
        )
    }

    private fun textContent(value: IrExpression, builder: DeclarationIrBuilder): IrFunctionExpression {
        val type = context.irBuiltIns.functionN(0).typeWith(context.irBuiltIns.unitType)
        return builder.irLambda(context, type) {
            body = DeclarationIrBuilder(context, symbol).irBlockBody {
                +irCallWithArgs(symbols.text, value.deepCopyWithSymbols())
            }
        }
    }
}

private fun IrFunction.parameter(name: String): IrValueParameter? =
    parameters.firstOrNull { it.name.asString() == name }

private fun IrCall.argument(parameter: IrValueParameter?): IrExpression? = parameter?.let { arguments[it] }
