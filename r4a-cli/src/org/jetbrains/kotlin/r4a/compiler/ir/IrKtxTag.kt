package org.jetbrains.kotlin.r4a.compiler.ir;

import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class IrKtxTag(val element: KtxElement, var body: Collection<IrStatement>?, var attributes: Collection<IrKtxAttribute>, val openTagExpr: IrExpression?) : IrKtxStatement {
    override val startOffset = element.startOffset
    override val endOffset = element.endOffset
    val bodyLambdaPsi by lazy { element.bodyLambdaExpression }
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitKtxStatement(this, data);
    }
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        if (openTagExpr != null) {
            openTagExpr.accept(visitor, data)
        }
        for(attribute in attributes) attribute.accept(visitor, data)
        body?.let { body -> for(statement in body) statement.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        this.body = body?.map { it.transform(transformer, data) }
        this.attributes = attributes.map { IrKtxAttribute(it.element, it.value?.transform(transformer, data)) }
    }
}
