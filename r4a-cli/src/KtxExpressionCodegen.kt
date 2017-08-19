package org.jetbrains.kotlin.r4a

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.MemberCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

class KtxExpressionCodegen(
      mv: MethodVisitor,
      frameMap: FrameMap,
      returnType: Type,
      context: MethodContext,
      state: GenerationState,
      parentCodegen: MemberCodegen<*>
) : ExpressionCodegen(mv, frameMap, returnType, context, state, parentCodegen) {

    override fun visitKtxElement(element: KtxElement, data: StackValue?): StackValue {
        val descriptor = context.thisDescriptor

        // TODO: Ask Yan why this is getting rewritten
        val dslBuilderInternalClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/R4aElementBuilderDSL"

        val tagType = bindingContext.get(
                BindingContext.KTX_TAG_TYPE_DESCRIPTOR,
                element
        ) as ClassifierDescriptor? ?: throw NullPointerException("KTX tag does not know descriptor: " + element.text)

        v.visitVarInsn(Opcodes.ALOAD, 0)
        v.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, this.asmType(descriptor.defaultType).internalName, "getMarkupBuilder",
                "()L$dslBuilderInternalClassName;", false
        )
        v.visitInsn(Opcodes.DUP)
        v.visitLdcInsn(Type.getType("L" + this.asmType(tagType.defaultType).internalName + ";"))
        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "startComponent", "(Ljava/lang/Class;)V", false)
        for (attribute in element.attributes!!) {
            v.visitInsn(Opcodes.DUP)
            v.visitLdcInsn(attribute.key!!.text)
            if (attribute.isStaticStringValue) {
                var text = attribute.value!!.text
                text = text.substring(1, text.length - 1) // Strip off the open/close quotes
                v.visitLdcInsn(text)
            } else
                gen(attribute.value as KtElement?, Type.getObjectType("java/lang/Object"))
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "attribute", "(Ljava/lang/String;Ljava/lang/Object;)V", false)
        }

        if (element.body != null) {
            generateBlock(element.body!!, true, null, null).put(Type.VOID_TYPE, v)
        }

        v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, dslBuilderInternalClassName, "endComponent", "()V", false)

        return StackValue.none()
    }

    fun visitStartOfFunction() {
        if (!context.hasThisDescriptor()) return
        val name = context.functionDescriptor.name
        if (context.functionDescriptor.name != Name.identifier("render")) return

        val descriptor = context.thisDescriptor
        val dslBuilderInternalClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/R4aElementBuilderDSL"
        v.visitVarInsn(Opcodes.ALOAD, 0)
        v.visitTypeInsn(Opcodes.NEW, dslBuilderInternalClassName)
        v.visitInsn(Opcodes.DUP)
        v.visitMethodInsn(Opcodes.INVOKESPECIAL, dslBuilderInternalClassName, "<init>", "()V", false)
        v.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, this.asmType(descriptor.defaultType).internalName, "setMarkupBuilder",
                "(L$dslBuilderInternalClassName;)V", false
        )
    }
}