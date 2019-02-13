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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.r4a.R4aUtils.generateR4APackageName
import org.jetbrains.kotlin.resolve.descriptorUtil.module


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

        if(!isComposable(declaration)) return declaration
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

        if(declaration.descriptor.annotations.hasAnnotation(R4aUtils.r4aFqName("Composable"))) return true

        var isComposable = false
        declaration.accept(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                if(isComposable) return
                element.acceptChildren(this, null)
            }

            override fun visitCall(expression: IrCall) {

                // TODO: When we change to jetbrains proposal, the synthetic calls will be more accurately identifiable.
                if(expression.descriptor.name == Name.identifier("emit") || expression.descriptor.name == Name.identifier("call")) {
                    isComposable = true
                }
                else super.visitCall(expression)
            }

            override fun visitFunction(declaration: IrFunction) {
                // End traversal; do not call super.
            }

        }, null)
        if(isComposable) return true

        return false
    }
}
