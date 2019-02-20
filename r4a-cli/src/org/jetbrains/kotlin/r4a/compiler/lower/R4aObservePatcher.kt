/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.r4a.ComposableAnnotationChecker
import org.jetbrains.kotlin.r4a.R4aUtils.generateR4APackageName
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil


class R4aObservePatcher(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        return super.visitConstructor(declaration)
    }

    fun generateLambda(type: IrType, declaration: IrFunction): IrBlockImpl {

        val lambdaDescriptor = AnonymousFunctionDescriptor(
            declaration.descriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false
        )

        lambdaDescriptor.initialize(
            null,
            null,
            emptyList(),
            emptyList(),
            type.toKotlinType(),
            Modality.FINAL,
            Visibilities.LOCAL
        )

        val lambdaFunctionImpl = IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            lambdaDescriptor,
            context.irBuiltIns.unitType,
            declaration.body)

        val lambdaFunctionReference = IrFunctionReferenceImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            lambdaFunctionImpl.symbol
            ,
            lambdaDescriptor,
            0,
            0,
            IrStatementOrigin.LAMBDA)

        return IrBlockImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type,
            IrStatementOrigin.LAMBDA,
            listOf(lambdaFunctionImpl, lambdaFunctionReference))
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {

        super.visitFunction(declaration)
        if(!isComposable(declaration)) return declaration
        declaration.descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if(InlineUtil.isInlinedArgument(it, context.state.bindingContext, true))
                    return declaration
            }
        }
        if(declaration.body == null) return declaration;

        val module = declaration.descriptor.module
        val observeFunctionDescriptor = module.getPackage(FqName(generateR4APackageName())).memberScope.getContributedFunctions(Name.identifier("Observe"), NoLookupLocation.FROM_BACKEND).single()

        val observeFunctionSymbol = context.ir.symbols.externalSymbolTable.referenceSimpleFunction(observeFunctionDescriptor)
        val observeFunctionCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, observeFunctionDescriptor.returnType!!.toIrType()!!, observeFunctionSymbol)
        observeFunctionCall.putValueArgument(0, generateLambda(observeFunctionDescriptor.valueParameters.get(0).type.toIrType()!!, declaration))

        declaration.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(observeFunctionCall))

        return declaration
    }

    fun isComposable(declaration: IrFunction): Boolean {
        val tmpTrace = DelegatingBindingTrace(context.state.bindingContext, "tmp for composable analysis")
        val composability = ComposableAnnotationChecker(ComposableAnnotationChecker.Mode.CHECKED).analyze(tmpTrace, declaration.descriptor)
        return when(composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }
}
