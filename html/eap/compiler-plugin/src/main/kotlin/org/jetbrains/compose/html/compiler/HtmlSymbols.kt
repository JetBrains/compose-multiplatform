/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import org.jetbrains.kotlin.backend.common.extensions.DeclarationFinder
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/** Runtime declarations required by the lowering. Resolve them once before visiting the module. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class HtmlSymbols private constructor(
    // Content reached from this resolved entry point enables pregeneration.
    val composeHtmlToStringSymbol: IrSimpleFunctionSymbol,
    // PrecomputedHtmlFragment(...) stores a precomputed subtree.
    val fragmentConstructorSymbol: IrConstructorSymbol,
    // LoweredStaticHtmlFragment(...) renders the stored HTML or its original browser content.
    val renderFragmentSymbol: IrSimpleFunctionSymbol,
    // @LowerableHtmlTag identifies tag wrappers that the plugin may lower.
    val tagMarkerSymbol: IrClassSymbol,
    val textSymbol: IrSimpleFunctionSymbol,
    val classesSymbol: IrSimpleFunctionSymbol,
    val idSymbol: IrSimpleFunctionSymbol,
    val attrSymbol: IrSimpleFunctionSymbol,
    val composableConstructorSymbol: IrConstructorSymbol,
) {
    companion object {
        private val htmlPackage = FqName("org.jetbrains.compose.web.dom")
        private val attrsScope = ClassId.topLevel(FqName("org.jetbrains.compose.web.attributes.AttrsScope"))
        private val composable = ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))

        /** Missing or ambiguous declarations leave the module unchanged. */
        fun resolve(finder: DeclarationFinder): HtmlSymbols? {
            return HtmlSymbols(
                composeHtmlToStringSymbol = finder.findFunctions(
                    CallableId(FqName("org.jetbrains.compose.web"), Name.identifier("composeHtmlToString"))
                ).singleOrNull() ?: return null,
                fragmentConstructorSymbol = finder.findConstructors(
                    ClassId.topLevel(htmlPackage.child(Name.identifier("PrecomputedHtmlFragment")))
                ).singleOrNull() ?: return null,
                renderFragmentSymbol = finder.htmlFunction("LoweredStaticHtmlFragment") ?: return null,
                tagMarkerSymbol = finder.findClass(
                    ClassId.topLevel(htmlPackage.child(Name.identifier("LowerableHtmlTag")))
                ) ?: return null,
                textSymbol = finder.htmlFunction("Text") ?: return null,
                classesSymbol = finder.findFunctions(CallableId(attrsScope, Name.identifier("classes")))
                    .singleOrNull { function -> function.owner.parameters.any { it.varargElementType != null } }
                    ?: return null,
                idSymbol = finder.findFunctions(CallableId(attrsScope, Name.identifier("id"))).singleOrNull()
                    ?: return null,
                attrSymbol = finder.findFunctions(CallableId(attrsScope, Name.identifier("attr"))).singleOrNull()
                    ?: return null,
                composableConstructorSymbol = finder.findConstructors(composable).singleOrNull() ?: return null,
            )
        }

        private fun DeclarationFinder.htmlFunction(name: String): IrSimpleFunctionSymbol? =
            findFunctions(CallableId(htmlPackage, Name.identifier(name))).singleOrNull()
    }
}
