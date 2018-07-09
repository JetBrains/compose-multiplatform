package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.backend.common.validateIrFile
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxAttribute
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.compiler.ir.find
import org.jetbrains.kotlin.r4a.compiler.lower.lowerComponentClass
import org.jetbrains.kotlin.r4a.compiler.lower.lowerComposeFunction
import java.util.*

class R4ASyntheticIrExtension : SyntheticIrExtension {

    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val openTagName = element.simpleTagName ?: element.qualifiedTagName ?: throw Exception("malformed element")
        var openTagExpr: IrExpression? = null
        try {
            openTagExpr = openTagName.accept(statementGenerator, null) as IrExpression?
        } catch (e: Exception) {

        }
        val body = element.body?.map { it.accept(statementGenerator, null) }
        val attributes = element.attributes!!.map {
            IrKtxAttribute(it, it.value?.accept(statementGenerator, null) as IrExpression?)
        }
        return IrKtxTag(element, body, attributes, openTagExpr)
    }

    override fun interceptModuleFragment(context: GeneratorContext, ktFiles: Collection<KtFile>, irModuleFragment: IrModuleFragment) {
        class TaggedFunction(val fn: IrFunction) { var hasKtx = false }

        val functions = LinkedList<TaggedFunction>()
        val tasks = mutableListOf<()->Unit>()

        // Lowering should not be done during traversal (concurrent modification); collect work to be done, defer execution
        irModuleFragment.accept(object : IrElementVisitorVoid {

            val classes = LinkedList<IrClass>()

            override fun visitElement(element: IrElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFunction(declaration: IrFunction) {
                functions.push(TaggedFunction(declaration))
                super.visitFunction(declaration)
                val fn = functions.pop()
                if (fn.hasKtx) {
                    val containingClass = classes.last
                    tasks.add({lowerComposeFunction(context, containingClass, fn.fn)})
                }
            }

            override fun visitClass(declaration: IrClass) {
                classes.push(declaration)
                super.visitClass(declaration)
                classes.pop()
                if (ComponentMetadata.isR4AComponent(declaration.descriptor)) {
                    tasks.add({lowerComponentClass(context, ComponentMetadata.fromDescriptor(declaration.descriptor), declaration)})
                }
            }

            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
                functions.peek().hasKtx = true
                expression.acceptChildren(this, null)
            }
        }, null)

        // Execute the collected tasks
        for(task in tasks) task()
    }
}
