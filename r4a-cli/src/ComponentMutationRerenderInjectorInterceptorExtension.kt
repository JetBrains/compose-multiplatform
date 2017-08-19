package org.jetbrains.kotlin.r4a

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.*
import java.util.HashMap

class ComponentMutationRerenderInjectorInterceptorExtension() : ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
            interceptedFactory: ClassBuilderFactory,
            bindingContext: BindingContext,
            diagnostics: DiagnosticSink
    ): ClassBuilderFactory {
        return AndroidOnDestroyClassBuilderFactory(interceptedFactory, bindingContext)
    }

    private inner class AndroidOnDestroyClassBuilderFactory(
            private val delegateFactory: ClassBuilderFactory,
            val bindingContext: BindingContext
    ) : ClassBuilderFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            return AndroidOnDestroyCollectorClassBuilder(delegateFactory.newClassBuilder(origin), bindingContext)
        }

        override fun getClassBuilderMode() = delegateFactory.classBuilderMode

        override fun asText(builder: ClassBuilder?): String? {
            return delegateFactory.asText((builder as AndroidOnDestroyCollectorClassBuilder).delegateClassBuilder)
        }

        override fun asBytes(builder: ClassBuilder?): ByteArray? {
            return delegateFactory.asBytes((builder as AndroidOnDestroyCollectorClassBuilder).delegateClassBuilder)
        }

        override fun close() {
            delegateFactory.close()
        }
    }

    private inner class AndroidOnDestroyCollectorClassBuilder(
            internal val delegateClassBuilder: ClassBuilder,
            val bindingContext: BindingContext
    ) : DelegatingClassBuilder() {
        private var currentClass: KtClass? = null
        private var currentClassName: String? = null
        private var definedClassName: String? = null

        override fun getDelegate() = delegateClassBuilder

        override fun defineClass(
                origin: PsiElement?,
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String,
                interfaces: Array<out String>
        ) {
            if (origin is KtClass) {
                currentClass = origin
                currentClassName = name
            }
            definedClassName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                methodName: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            return object : MethodVisitor(Opcodes.ASM5, super.newMethod(origin, access, methodName, desc, signature, exceptions)) {
                override fun visitFieldInsn(opcode: Int, owner: String?, fieldName: String?, desc: String?) {

                    if((bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, currentClass) != null) && opcode == Opcodes.PUTFIELD && methodName != "renderIntoViewGroup" && !methodName.startsWith("\$\$")) {
                        super.visitInsn(Opcodes.SWAP)
                        super.visitInsn(Opcodes.DUP_X1)
                        super.visitInsn(Opcodes.SWAP)
                        super.visitFieldInsn(opcode, owner, fieldName, desc)

                        /*
                        // TODO: Verify that we are not performing more renders than strictly necessary
                        // TODO: in particular, we should do a check to verify that the PUTFIELD actually resulted in a change (ie. oldValue != newValue)
                        super.visitTypeInsn(Opcodes.NEW, "java/lang/Throwable");
                        super.visitInsn(Opcodes.DUP);
                        super.visitLdcInsn("Scheduling rerender");
                        super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Throwable", "<init>", "(Ljava/lang/String;)V", false);
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
                        */

                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, "rerender", "()V", false);
                    } else {
                        super.visitFieldInsn(opcode, owner, fieldName, desc)
                    }
                }
            }
        }
    }
}