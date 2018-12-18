package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.psi2ir.generators.pregenerateCallReceivers
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxAttribute
import org.jetbrains.kotlin.r4a.compiler.ir.IrKtxTag
import org.jetbrains.kotlin.r4a.compiler.lower.generateChildrenLambda
import org.jetbrains.kotlin.r4a.compiler.lower.lowerComposeFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.source.toSourceElement
import java.util.*

class R4ASyntheticIrExtension : SyntheticIrExtension {

    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val tagInfo = statementGenerator.context.bindingContext.get(R4AWritableSlices.KTX_TAG_INFO, element) ?: error("no tag info")
        val openTagName = element.simpleTagName ?: element.qualifiedTagName ?: error("malformed element")

        val tagCall = statementGenerator.pregenerateCallReceivers(tagInfo.resolvedCall)
        val functionDescriptor = tagInfo.resolvedCall.resultingDescriptor
        val callExpr = tagCall.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
            val returnType = functionDescriptor.returnType!!
            val functionSymbol = statementGenerator.context.symbolTable.referenceFunction(functionDescriptor.original)
            IrCallImpl(
                openTagName.startOffset, openTagName.endOffset,
                returnType.toIrType()!!,
                functionSymbol,
                functionDescriptor as FunctionDescriptor,
                tagCall.typeArguments?.count() ?: 0,
                IrStatementOrigin.INVOKE,
                null
            ).apply {
                dispatchReceiver = dispatchReceiverValue?.load()
                extensionReceiver = extensionReceiverValue?.load()
            }
        } as IrCallImpl

        val attributes = element.attributes.map { attr ->
            val valueExpr = attr.value ?: attr.key ?: error("malformed element")
            val irValueExpr = KtPsiUtil.safeDeparenthesize(valueExpr).accept(statementGenerator, null) as? IrExpression ?: error("attributes need to be expressions")
            IrKtxAttribute(attr, irValueExpr)
        }

        var body: List<IrStatement>? = null
        var accessesToCapture: List<IrDeclarationReference> = emptyList()
        var irParameters: List<IrValueParameter> = emptyList()

        val bodyStatements = element.body
        val lambda = element.bodyLambdaExpression

        if (bodyStatements != null && lambda != null) {
            val context = statementGenerator.context
            val symbolTable = context.symbolTable

            val lambdaDescriptor = context.bindingContext.get(DECLARATION_TO_DESCRIPTOR, lambda.functionLiteral) ?: null

            if (lambdaDescriptor == null) {
                body = bodyStatements.map { it.accept(statementGenerator, null) }
            } else {
                symbolTable.withScope(lambdaDescriptor) {
                    val symbolsDefined = mutableSetOf<IrSymbol>()
                    val symbolAccesses = mutableSetOf<IrDeclarationReference>()

                    irParameters = lambda.valueParameters.mapIndexed { index, parameter ->
                        val parameterDescriptor =
                            context.bindingContext.get(DECLARATION_TO_DESCRIPTOR, parameter) as? ValueParameterDescriptor
                                    ?: error("expected parameter descriptor")
                        symbolTable.declareValueParameter(
                            parameter.startOffset,
                            parameter.endOffset,
                            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
                            parameterDescriptor,
                            parameterDescriptor.type.toIrType()!!
                        ).apply {
                            symbolsDefined.add(symbol)
                        }
                    }

                    val statements = bodyStatements.map { it.accept(statementGenerator, null) }

                    for (statement in statements) {
                        statement.accept(object : IrElementVisitorVoid {
                            override fun visitElement(element: IrElement) {
                                element.acceptChildren(this, null)
                            }

                            override fun visitVariable(declaration: IrVariable) {
                                symbolsDefined.add(declaration.symbol)
                                declaration.acceptChildren(this, null)
                            }

                            override fun visitValueParameter(declaration: IrValueParameter) {
                                symbolsDefined.add(declaration.symbol)
                            }

                            override fun visitGetValue(expression: IrGetValue) {
                                symbolAccesses.add(expression)
                            }

                            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
                                expression.acceptChildren(this, data)
                            }
                        }, null)
                    }

                    accessesToCapture = symbolAccesses.filter {
                        !symbolsDefined.contains(it.symbol)
                    }.toList()

                    body = statements
                }
            }
        }

        return IrKtxTag(
            element,
            body,
            accessesToCapture,
            irParameters,
            attributes,
            callExpr
        )
    }

    override fun interceptModuleFragment(context: GeneratorContext, ktFiles: Collection<KtFile>, irModuleFragment: IrModuleFragment) {
        class TaggedFunction(val fn: IrFunction) {
            var hasKtx = false
        }

        val tasks = mutableListOf<() -> Unit>()

        // Lowering should not be done during traversal (concurrent modification); collect work to be done, defer execution
        irModuleFragment.accept(object : IrElementVisitorVoid {

            val functions = LinkedList<TaggedFunction>()
            val classes = LinkedList<IrClass>()
            val containers = LinkedList<IrPackageFragment>()

            override fun visitElement(element: IrElement) {
                element.acceptChildren(this, null)
            }

            override fun visitFile(declaration: IrFile) {
                containers.push(declaration)
                super.visitFile(declaration)
                containers.pop()
            }

            override fun visitPackageFragment(declaration: IrPackageFragment) {
                containers.push(declaration)
                super.visitPackageFragment(declaration)
                containers.pop()
            }

            override fun visitFunction(declaration: IrFunction) {
                functions.push(TaggedFunction(declaration))
                super.visitFunction(declaration)
                val fn = functions.pop()
                if (fn.hasKtx) {
                    val container = containers.last
                    tasks.add { lowerComposeFunction(context, container, fn.fn) }
                }
            }

            override fun visitClass(declaration: IrClass) {
                classes.push(declaration)
                super.visitClass(declaration)
                classes.pop()
            }

            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
                functions.peek().hasKtx = true
                expression.acceptChildren(this, null)
            }
        }, null)

        // Execute the collected tasks
        for (task in tasks) task()
    }
}
