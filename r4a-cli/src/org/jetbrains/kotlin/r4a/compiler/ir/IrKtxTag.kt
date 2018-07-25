package org.jetbrains.kotlin.r4a.compiler.ir;

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class IrKtxTag(
    val element: KtxElement,
    var body: Collection<IrStatement>?,
    var capturedExpressions: List<IrDeclarationReference>,
    var parameters: List<IrValueParameter>,
    var attributes: Collection<IrKtxAttribute>,
    val callExpr: IrCallImpl
) : IrKtxStatement {
    override val startOffset = element.startOffset
    override val endOffset = element.endOffset

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitKtxStatement(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        for (attribute in attributes) attribute.accept(visitor, data)
        for (parameter in parameters) parameter.accept(visitor, data)
        body?.let { body -> for (statement in body) statement.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        attributes = attributes.map {
            IrKtxAttribute(it.element, it.value.transform(transformer, data))
        }

        parameters = parameters.map { it.transform(transformer, data) }

        val nextBody = body?.map { it.transform(transformer, data) }

        capturedExpressions = getCapturedAccesses(nextBody, parameters)

        body = nextBody
    }
}


fun getCapturedAccesses(bodyStatements: List<IrStatement>?, parameters: List<IrValueParameter>): List<IrDeclarationReference> {
    if (bodyStatements == null) return emptyList()

    val symbolsDefined = mutableSetOf<IrSymbol>()
    val symbolAccesses = mutableSetOf<IrDeclarationReference>()
    val visitor = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        override fun visitVariable(declaration: IrVariable) {
            symbolsDefined.add(declaration.symbol)
        }

        override fun visitValueParameter(declaration: IrValueParameter) {
            symbolsDefined.add(declaration.symbol)
        }

        override fun visitVariableAccess(expression: IrValueAccessExpression) {
            symbolAccesses.add(expression)
        }

        override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
            expression.acceptChildren(this, data)
        }
    }

    // NOTE: we DON'T traverse the attributes here, as they will be executed outside of the children lambda
    for (parameter in parameters) parameter.accept(visitor, null)
    for (statement in bodyStatements) statement.accept(visitor, null)

    return symbolAccesses.filter {
        !symbolsDefined.contains(it.symbol) // && it.descriptor !in parameterDescriptors
    }.toList()
}
