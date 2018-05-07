/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.compiler.lower

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.compiler.ir.buildWithScope

// TODO: Create lower function for user when companion already exists.
fun generateComponentCompanionObject(context: GeneratorContext, componentMetadata: ComponentMetadata): IrClass {
    val companion = context.symbolTable.declareClass(-1, -1, IrDeclarationOrigin.DEFINED, (componentMetadata.descriptor as ClassDescriptor).companionObjectDescriptor!!)
    val createInstanceFunction = context.symbolTable.declareSimpleFunction(-1, -1, IrDeclarationOrigin.DEFINED, companion.descriptor.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("createInstance"), NoLookupLocation.FROM_BACKEND).single())
        .buildWithScope(context) { irFunction ->

            irFunction.createParameterDeclarations()

            val wrapperViewInstance = IrCallImpl(-1, -1, context.symbolTable.referenceConstructor(componentMetadata.wrapperViewDescriptor.unsubstitutedPrimaryConstructor))
            wrapperViewInstance.putValueArgument(0, IrGetValueImpl(-1, -1, irFunction.valueParameters[0].symbol))
            irFunction.body = IrBlockBodyImpl(-1, -1, listOf(IrReturnImpl(-1, -1, irFunction.symbol, wrapperViewInstance)))
        }
    companion.declarations.add(createInstanceFunction)
    return companion
}

