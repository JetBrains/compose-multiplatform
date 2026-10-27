/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.html.compiler.lowering

import org.jetbrains.compose.html.compiler.HtmlSymbols
import org.jetbrains.compose.html.compiler.ir.anyDescendant
import org.jetbrains.compose.html.compiler.ir.irLambda
import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.supportsPreparedHtmlAttributes
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/**
 * Caches constant attributes when an element still needs runtime rendering.
 * The attributes are stored in a shared field.
 *
 * ```kotlin
 * @Composable
 * fun UserName(name: @Composable () -> String) {
 *     Span(attrs = { classes("user") }) {
 *         Text(name())
 *     }
 * }
 * private val __htmlAttributes0 = /* prepared attributes */
 *
 * @Composable
 * fun UserName(name: @Composable () -> String) {
 *     PreparedTextElement(__htmlAttributes0) {
 *         name()
 *     }
 * }
 * ```
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class PreparedElementLowering(
    private val context: IrPluginContext,
    private val symbols: HtmlSymbols,
    private val generatedFields: HtmlFields,
) {
    private data class AttributeKey(val type: IrType, val tag: String, val attributes: LiteralAttributes)
    private data class WrapperTypes(val attrs: IrType, val receiver: IrType, val element: IrType)

    private val sharedAttributes = mutableMapOf<AttributeKey, IrField>()
    val fields: Collection<IrField> get() = sharedAttributes.values

    // Reuses constant attributes for supported HTML wrappers. Returns null if this is not possible.
    fun lower(element: HtmlElement, builder: DeclarationIrBuilder): IrExpression? {
        val attributes = element.attributes as? LiteralAttributes ?: return null
        if (element.namespace != HtmlNamespace || element.call.arguments.size != 2 ||
            !supportsPreparedHtmlAttributes(element.tag)) return null
        if (attributes.values.size + attributes.classNames.size > MaxPreparedAttributeOperations ||
            sharedAttributes.size >= MaxPreparedAttributesPerFile) return null
        val types = wrapperTypes(element.call) ?: return null
        val values = attributes.values
        if (!generatedFields.canStore(values.keys + values.values + attributes.classNames +
                listOf(requireNotNull(element.openingTag)))) return null
        val attrs = prepareAttributes(element, attributes, types)
        // Read content after the transformer has lowered the children.
        val content = element.call.arguments[1] as? IrFunctionExpression
        if (content == null) {
            // Stored or nullable content keeps its original wrapper and argument evaluation.
            element.call.arguments[0] = builder.irCall(symbols.attributesCallback).apply {
                type = types.attrs
                arguments[0] = builder.irGetField(null, attrs)
            }
            return element.call
        }
        val text = (content.function.body as? IrBlockBody)?.statements?.singleOrNull() as? IrCall
        val value = text?.takeIf { it.symbol == symbols.text }?.arguments?.singleOrNull()
        val target = if (value != null && !returnsFromContent(value, content)) {
            // Reuse the function and receiver symbols, including references in nested lambdas.
            content.function.returnType = context.irBuiltIns.stringType
            content.type = parameterType(symbols.preparedTextElement, 1, types.element)
            val bodyBuilder = DeclarationIrBuilder(context, content.function.symbol, value.startOffset, value.endOffset)
            content.function.body = bodyBuilder.irBlockBody { +irReturn(value) }
            symbols.preparedTextElement
        } else {
            symbols.preparedContainer
        }
        return builder.irCall(target).apply {
            typeArguments[0] = types.element
            arguments[0] = builder.irGetField(null, attrs)
            arguments[1] = content
        }
    }

    private fun wrapperTypes(call: IrCall): WrapperTypes? {
        // The wrapper accepts Function1<AttrsScope<TElement>, Unit>.
        val attrs = call.symbol.owner.parameters[0].type.makeNotNull()
        val receiver = (attrs as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull ?: return null
        val element = (receiver as? IrSimpleType)?.arguments?.singleOrNull()?.typeOrNull ?: return null
        return WrapperTypes(attrs, receiver, element)
    }

    private fun returnsFromContent(value: IrExpression, content: IrFunctionExpression): Boolean =
        value.anyDescendant { it is IrReturn && it.returnTargetSymbol == content.function.symbol }

    private fun parameterType(symbol: IrSimpleFunctionSymbol, index: Int, elementType: IrType): IrType =
        IrTypeSubstitutor(
            symbol.owner.typeParameters.map { it.symbol },
            listOf(makeTypeProjection(elementType, Variance.INVARIANT)),
        ).substitute(symbol.owner.parameters[index].type)

    // Reuse one field for the same element type, tag and attributes in this file.
    private fun prepareAttributes(element: HtmlElement, attributes: LiteralAttributes, types: WrapperTypes): IrField {
        val type = requireNotNull(symbols.preparedHtmlAttributes.owner.returnType.classOrNull).typeWith(types.element)
        return sharedAttributes.getOrPut(AttributeKey(type, element.tag, attributes)) {
            val field = generatedFields.create(
                "__htmlAttributes", sharedAttributes.size, type, element.call.startOffset, element.call.endOffset
            )
            val builder = DeclarationIrBuilder(context, field.symbol, element.call.startOffset, element.call.endOffset)
            field.initializer = context.irFactory.createExpressionBody(
                builder.irCall(symbols.preparedHtmlAttributes).apply {
                    this.type = type
                    typeArguments[0] = types.element
                    arguments[0] = builder.irString(element.tag)
                    arguments[1] = generatedFields.string(requireNotNull(element.openingTag), builder)
                    arguments[2] = attributeInitializer(attributes, types, builder)
                }
            )
            field.patchDeclarationParents(field.parent)
            field
        }
    }

    private fun attributeInitializer(
        attributes: LiteralAttributes,
        types: WrapperTypes,
        parent: DeclarationIrBuilder,
    ): IrExpression = parent.irLambda(context, types.attrs) {
        // Rebuild from folded values so file initialization never captures render-time locals.
        val receiver = buildValueParameter(this) {
            name = Name.special("<this>")
            type = types.receiver
            kind = IrParameterKind.ExtensionReceiver
        }
        parameters = listOf(receiver)
        val builder = DeclarationIrBuilder(context, symbol, parent.startOffset, parent.endOffset)
        body = builder.irBlockBody {
            for ((name, value) in attributes.values) {
                +builder.irCall(symbols.attr).apply {
                    type = types.receiver
                    arguments[0] = builder.irGet(receiver)
                    arguments[1] = generatedFields.string(name, builder)
                    arguments[2] = generatedFields.string(value, builder)
                }
            }
            if (attributes.classNames.isNotEmpty()) {
                +builder.irCall(symbols.classes).apply {
                    arguments[0] = builder.irGet(receiver)
                    arguments[1] = builder.irVararg(
                        context.irBuiltIns.stringType, attributes.classNames.map { generatedFields.string(it, builder) }
                    )
                }
            }
        }
    }
}
