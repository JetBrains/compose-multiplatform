package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor

class KtxNodeCodegenExtension() : ExpressionCodegenExtension {
    override fun createFunctionGenerationStrategy(state: GenerationState, declaration: KtDeclarationWithBody): FunctionGenerationStrategy? {
        val bodyExpression = declaration.bodyExpression ?: error("Function has no body: " + declaration.getElementTextWithContext())
        if (declaration.getName() != "render") return null; // TODO: Be smarter about our check
        return object : FunctionGenerationStrategy() {
            override fun generateBody(
                mv: MethodVisitor,
                frameMap: FrameMap,
                signature: JvmMethodSignature,
                context: MethodContext,
                parentCodegen: MemberCodegen<*>
            ) {
                val codegen = KtxExpressionCodegen(mv, frameMap, signature.getReturnType(), context, state, parentCodegen)
                codegen.visitStartOfFunction();
                codegen.returnExpression(bodyExpression)
            }

            override fun skipNotNullAssertionsForParameters(): Boolean = false
        }
    }
}