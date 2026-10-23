/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.ir

import java.util.Collections
import java.util.IdentityHashMap
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames

private val Composable = FqName("androidx.compose.runtime.Composable")
internal val KotlinLet = FqName("kotlin.let")
internal val KotlinRepeat = FqName("kotlin.repeat")

// Checks this element too and stops at the first match.
internal fun IrElement.anyDescendant(predicate: (IrElement) -> Boolean): Boolean {
    var found = false
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (found) return
            found = predicate(element)
            if (!found) element.acceptChildrenVoid(this)
        }
    })
    return found
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun hasComposableCall(expression: IrExpression?): Boolean =
    expression?.anyDescendant {
        it is IrCall && (it.symbol.owner.hasAnnotation(Composable) ||
            it.dispatchReceiver?.type?.hasAnnotation(Composable) == true)
    } == true

internal fun IrExpression?.isAbsent(): Boolean = this == null || (this is IrConst && kind == IrConstKind.Null)

internal fun IrBuilderWithScope.irCallWithArgs(symbol: IrSimpleFunctionSymbol, vararg values: IrExpression?): IrCall =
    irCall(symbol).apply {
        values.forEachIndexed { index, value -> arguments[index] = value }
    }

// Collects value references and return targets in this subtree, including nested lambdas.
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrElement.contentScopeReferences(): Set<IrSymbol> {
    val referenced: MutableSet<IrSymbol> = Collections.newSetFromMap(IdentityHashMap())
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            when (element) {
                is IrGetValue -> referenced.add(element.symbol)
                is IrReturn -> referenced.add(element.returnTargetSymbol)
            }
            element.acceptChildrenVoid(this)
        }
    })
    return referenced
}

// Unwrap only this lambda's return. Keep non-local returns for the caller to reject.
internal fun IrStatement.unwrapReturn(target: IrFunctionSymbol): IrStatement =
    if (this is IrReturn && returnTargetSymbol == target) value else this

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun DeclarationIrBuilder.irLambda(
    context: IrPluginContext,
    type: IrType,
    build: IrSimpleFunction.() -> Unit,
): IrFunctionExpression {
    val function = context.irFactory.buildFun {
        name = SpecialNames.ANONYMOUS
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        visibility = DescriptorVisibilities.LOCAL
        returnType = context.irBuiltIns.unitType
    }
    function.parent = scope.scopeOwnerSymbol.owner as IrDeclarationParent
    function.build()
    return IrFunctionExpressionImpl(startOffset, endOffset, type, function, IrStatementOrigin.LAMBDA)
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrSimpleFunctionSymbol.withOverrides(): Sequence<IrSimpleFunctionSymbol> = sequence {
    yield(this@withOverrides)
    for (symbol in owner.overriddenSymbols) yieldAll(symbol.withOverrides())
}
