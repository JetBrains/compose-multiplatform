/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import kotlin.properties.ReadOnlyProperty
import org.jetbrains.compose.html.compiler.ir.withOverrides
import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// Finds the runtime classes and functions used by the generated code.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlSymbols private constructor(resolver: Resolver) {
    private val finder = resolver.finder
    val formatNumber by resolver.function(FqName("org.jetbrains.compose.web.internal"), "formatNumber")
    val writeStaticHtml by resolver.htmlFunction("WriteStaticHtml")
    val beginStaticHtmlFragment by resolver.writerFunction("beginStaticHtmlFragment")
    val appendStaticHtmlChunk by resolver.writerFunction("appendStaticHtmlChunk")
    val preparedHtmlAttributes by resolver.htmlFunction("preparedHtmlAttributes")
    val attributesCallback by resolver.getter(HtmlPackage, "PreparedHtmlAttributes", "callback")
    val preparedTextElement by resolver.htmlFunction("PreparedTextElement")
    val preparedContainer by resolver.htmlFunction("PreparedContainer")
    val currentHtmlStringWriter by resolver.htmlFunction("currentHtmlStringWriter")
    val currentHtmlValidationIsStrict by resolver.htmlFunction("currentHtmlValidationIsStrict")
    val openLoweredRegion by resolver.writerFunction("openLoweredRegion")
    val advanceLoweredRegion by resolver.writerFunction("advanceLoweredRegion")
    val writeText by resolver.writerFunction("writeText")
    val closeElement by resolver.writerFunction("closeElement")
    val beginMixedElement by resolver.writerFunction("beginMixedElement")
    val writeMixedLiteral by resolver.writerFunction("writeMixedLiteral")
    val writeMixedAttribute by resolver.writerFunction("writeMixedAttribute")
    val mixedStyle by resolver.htmlFunction("mixedStyle")
    val writeMixedStyle by resolver.writerFunction("writeMixedStyle")
    val validateMixedClass by resolver.writerFunction("validateMixedClass")
    val writeMixedClass by resolver.writerFunction("writeMixedClass")
    val finishMixedClasses by resolver.writerFunction("finishMixedClasses")
    val finishMixedElement by resolver.writerFunction("finishMixedElement")
    val inputs by resolver.inputs()
    val inputTypeValue by resolver.getter(AttributesPackage, "InputType", "typeStr")
    val attributeMarker by resolver.htmlClass("LowerableAttribute")
    val anyToString by resolver.member(ClassId.topLevel(FqName("kotlin.Any")), "toString")
    val autoCompleteValue by resolver.htmlFunction("htmlAutoCompleteValue")
    val inputValueIsAttribute by resolver.htmlFunction("inputValueIsAttribute")

    // @LowerableHtmlTag identifies tag wrappers that the plugin may lower.
    val tagMarker by resolver.htmlClass("LowerableHtmlTag")
    val text by resolver.htmlFunction("Text")
    val classes by resolver.attrsFunction("classes", varargOnly = true)
    val id by resolver.attrsFunction("id")
    val attr by resolver.attrsFunction("attr")
    val style by resolver.attrsFunction("style")

    data class Attribute(
        val name: String,
        val conversion: IrSimpleFunctionSymbol?,
        val value: String,
        val defaultValue: String?
    )

    private val attributes = mutableMapOf<IrSimpleFunctionSymbol, Attribute?>()
    private val helpers = mutableMapOf<Pair<String, String>, List<IrSimpleFunctionSymbol>>()

    // Reads attribute metadata, including metadata inherited from an overridden helper.
    fun attribute(symbol: IrSimpleFunctionSymbol): Attribute? = attributes.getOrPut(symbol) {
        val declaration = symbol.withOverrides().firstOrNull { candidate ->
            candidate.owner.annotations.count { it.classSymbol == attributeMarker } == 1
        } ?: return@getOrPut null
        val marker = declaration.owner.annotations.single { it.classSymbol == attributeMarker }
        fun string(name: String) =
            (marker.argumentMapping[Name.identifier(name)] as? IrConst)?.value as? String ?: ""

        val property = string("conversion").ifEmpty {
            if (declaration.owner.parameters.size == 1 || declaration.owner.parameters.last().type.isString()) "" else "toString"
        }
        val conversion = when (property) {
            "" -> null
            "formatNumber" -> formatNumber
            "toString" -> anyToString
            "onOff" -> autoCompleteValue
            else -> {
                val owner = declaration.owner.parameters.last().type.classOrNull?.owner?.classId ?: return@getOrPut null
                finder.findProperties(CallableId(owner, Name.identifier(property)))
                    .singleOrNull()?.owner?.getter?.symbol
                    ?: return@getOrPut null
            }
        }
        val name = string("name").ifEmpty {
            val name = declaration.owner.name.asString()
            if (declaration.owner.fqNameWhenAvailable?.parent()
                    ?.asString() == SvgPackage.asString()
            ) name else name.lowercase()
        }
        val defaultValue = (marker.argumentMapping[Name.identifier("defaultValue")] as? IrVararg)
            ?.elements?.singleOrNull()?.let { (it as? IrConst)?.value as? String }
        Attribute(name, conversion, string("value"), defaultValue)
    }

    // Finds scope members and HTML or SVG extensions for a wrapper parameter.
    fun helpers(name: String, namespace: String, receiver: IrType): List<IrSimpleFunctionSymbol> {
        val pkg = if (namespace == SvgNamespace) SvgPackage else AttributesPackage
        val extensions = helpers.getOrPut(name to namespace) {
            finder.findFunctions(CallableId(pkg, Name.identifier(name))).toList()
        }
        val owner = receiver.classOrNull?.owner?.classId
        val members = owner?.let { finder.findFunctions(CallableId(it, Name.identifier(name))).toList() }.orEmpty()
        return members + extensions
    }

    companion object {
        val HtmlPackage = FqName("org.jetbrains.compose.web.dom")
        val AttributesPackage = FqName("org.jetbrains.compose.web.attributes")
        val SvgPackage = FqName("org.jetbrains.compose.web.svg")

        // Standard wrappers use compile-time type names, while generic Input uses the runtime identity check.
        val ValueAttributeInputTypes = setOf("checkbox", "radio", "hidden", "submit")
        const val SvgNamespace = "http://www.w3.org/2000/svg"
        const val InputScope = "org.jetbrains.compose.web.attributes.builders.InputAttrsScope"
        const val TextAreaScope = "org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope"
        const val SelectScope = "androidx.compose.web.attributes.SelectAttrsScope"
        const val EventsScope = "org.jetbrains.compose.web.attributes.EventsListenerScope"
        val AttrsScope = ClassId.topLevel(AttributesPackage.child(Name.identifier("AttrsScope")))
        val DirectTags =
            setOf(HtmlPackage.child(Name.identifier("TagElement")), HtmlPackage.child(Name.identifier("TagElementNS")))
        val CreateBuilder = FqName("org.jetbrains.compose.web.dom.ElementBuilder.Companion.createBuilder")
        val FormEvents = setOf(
            AttributesPackage.child(Name.identifier("onSubmit")),
            AttributesPackage.child(Name.identifier("onReset"))
        )

        // Returns null and reports all missing or ambiguous declarations if the runtime does not match.
        fun resolve(finder: DeclarationFinder, onUnavailable: (List<String>) -> Unit): HtmlSymbols? {
            val resolver = Resolver(finder)
            val symbols = HtmlSymbols(resolver)
            if (resolver.unavailable.isEmpty()) return symbols
            onUnavailable(resolver.unavailable)
            return null
        }
    }

    private class Resolver(val finder: DeclarationFinder) {
        val unavailable = mutableListOf<String>()

        // resolve checks every required declaration before these values can be read.
        fun <T : Any> required(name: String, value: T?): ReadOnlyProperty<HtmlSymbols, T> {
            if (value == null) unavailable.add(name)
            return ReadOnlyProperty { _, _ -> checkNotNull(value) { "Unresolved runtime declaration: $name" } }
        }

        fun function(pkg: FqName, name: String) = required(
            "$pkg.$name", finder.findFunctions(CallableId(pkg, Name.identifier(name))).singleOrNull()
        )

        fun htmlFunction(name: String) = function(HtmlPackage, name)

        fun htmlClass(name: String) = required(
            "$HtmlPackage.$name", finder.findClass(ClassId.topLevel(HtmlPackage.child(Name.identifier(name))))
        )

        fun member(owner: ClassId, name: String) = required(
            "${owner.asSingleFqName()}.$name",
            finder.findFunctions(CallableId(owner, Name.identifier(name))).singleOrNull()
        )

        fun writerFunction(name: String) =
            member(ClassId.topLevel(HtmlPackage.child(Name.identifier("HtmlStringWriterContext"))), name)

        fun getter(pkg: FqName, owner: String, name: String) = required(
            "$pkg.$owner.$name", finder.findProperties(
                CallableId(ClassId.topLevel(pkg.child(Name.identifier(owner))), Name.identifier(name))
            ).singleOrNull()?.owner?.getter?.symbol
        )

        fun attrsFunction(name: String, varargOnly: Boolean = false) = required(
            "${AttrsScope.asSingleFqName()}.$name${if (varargOnly) "(vararg)" else ""}",
            finder.findFunctions(CallableId(AttrsScope, Name.identifier(name))).singleOrNull {
                !varargOnly || it.owner.parameters.any { parameter -> parameter.varargElementType != null }
            }
        )

        fun inputs() = required(
            "$HtmlPackage.Input(type[, attrs])",
            finder.findFunctions(CallableId(HtmlPackage, Name.identifier("Input"))).filter {
                val parameters = it.owner.parameters
                val names = parameters.map { parameter -> parameter.name.asString() }
                val inputType = ClassId.topLevel(AttributesPackage.child(Name.identifier("InputType")))
                if (names !in listOf(listOf("type"), listOf("type", "attrs")) ||
                    parameters.first().type.classOrNull?.owner?.classId != inputType
                ) return@filter false
                val attrs = parameters.getOrNull(1)?.type as? IrSimpleType
                names.size == 1 || (
                        attrs?.classOrNull?.owner?.fqNameWhenAvailable == FqName("kotlin.Function1") &&
                                attrs.arguments.firstOrNull()?.typeOrNull?.classOrNull?.owner?.classId == ClassId.topLevel(
                            FqName(InputScope)
                        )
                        )
            }.toSet().takeIf { it.isNotEmpty() }
        )
    }
}
