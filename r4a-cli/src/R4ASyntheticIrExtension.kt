package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxAttribute
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.compiler.ir.find
import org.jetbrains.kotlin.r4a.compiler.lower.lowerComponentClass

class R4ASyntheticIrExtension : SyntheticIrExtension {

    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val body = element.body?.map { it.accept(statementGenerator, null) }
        val attributes = element.attributes!!.map {
            IrKtxAttribute(it, it.value?.accept(statementGenerator, null) as IrExpression?)
        }
        return IrKtxTag(element, body, attributes)
    }

    override fun interceptModuleFragment(context: GeneratorContext, ktFiles: Collection<KtFile>, irModuleFragment: IrModuleFragment) {
        val components = irModuleFragment.find { it is IrClass && ComponentMetadata.isR4AComponent(it.descriptor) }.map { it as IrClass }
        for(component in components) {
            lowerComponentClass(context, ComponentMetadata.fromDescriptor(component.descriptor), component)
        }
    }
}
