package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy.CodegenBased
import org.jetbrains.org.objectweb.asm.Opcodes

import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.DescriptorUtils.getDefaultConstructorVisibility
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

class StaticWrapperCreatorFunctionCodegenExtension() : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) = with(codegen) {
        val cls = codegen.descriptor

        val componentClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Component"
        if(cls.getSuperClassNotAny()?.fqNameSafe == FqName(componentClassName)) {
            val syntheticClassDescriptor = codegen.bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, cls.source.getPsi() as KtClass)!!
            writeCreatorClass(codegen, syntheticClassDescriptor)
            codegen.generateSyntheticInternalHelpers()
        }


        val containingClass = cls.containingDeclaration as? ClassDescriptor ?: return
        if (containingClass.getSuperClassNotAny()?.fqNameSafe == FqName(componentClassName)) {
            val syntheticClassDescriptor = codegen.bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, containingClass.source.getPsi() as KtClass)!!
            generateGetterMethodConstructor(syntheticClassDescriptor)
        }
    }

    private fun ImplementationBodyCodegen.generateGetterMethodConstructor(syntheticClassDescriptor : GeneratedViewClassDescriptor) {
     //   val newMethod = syntheticClassDescriptor.getInstanceCreatorFunction(descriptor)

        val newMethod = descriptor.unsubstitutedMemberScope
                .getContributedFunctions(Name.identifier("createInstance"), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
                .firstOrNull { true }

        newMethod!!.write(this) {
                this.v.visitTypeInsn(Opcodes.NEW, this.asmType(syntheticClassDescriptor.defaultType).internalName);
                this.v.visitInsn(Opcodes.DUP)
                this.v.visitVarInsn(Opcodes.ALOAD, 1) // TODO: Make this function static, so this would become ALOAD 0
                this.v.visitMethodInsn(Opcodes.INVOKESPECIAL, this.asmType(syntheticClassDescriptor.defaultType).internalName, "<init>", "(Landroid/content/Context;)V", false);
                this.v.visitInsn(Opcodes.ARETURN);
            }
    }

    private fun ImplementationBodyCodegen.generateSyntheticInternalHelpers() {

        this.v.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE, "elements", "Ljava/util/List;", "Ljava/util/List<+Lcom/google/r4a/Element;>;", null);


        val returnType : SimpleType = descriptor.builtIns.unitType
        val newMethod = SimpleFunctionDescriptorImpl.create(descriptor, Annotations.EMPTY, Name.identifier("renderIntoViewGroup"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
        val vgParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 0, Annotations.EMPTY,
                Name.identifier("container"),
                descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewGroup")))!!.defaultType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
        val startParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 1, Annotations.EMPTY,
                Name.identifier("startIndex"),
                descriptor.builtIns.intType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
        val endParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 2, Annotations.EMPTY,
                Name.identifier("endIndex"),
                descriptor.builtIns.intType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
        newMethod.initialize(null, if(descriptor.isCompanionObject) descriptor.thisAsReceiverParameter else null, emptyList(), listOf(vgParameter, startParameter, endParameter), returnType, Modality.OPEN, Visibilities.PUBLIC)

        val internalClassName = descriptor.fqNameSafe.topLevelClassInternalName()
        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, newMethod, object: CodegenBased(state) {
            override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                codegen.v.visitVarInsn(Opcodes.ALOAD, 0);
                codegen.v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "render", "()V", false);
                codegen.v.visitVarInsn(Opcodes.ALOAD, 0);
                codegen.v.visitFieldInsn(Opcodes.GETFIELD, internalClassName, "elements", "Ljava/util/List;");
                codegen.v.visitVarInsn(Opcodes.ALOAD, 0);
                val elementBuilderClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/R4aElementBuilderDSL"
                codegen.v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "getMarkupBuilder", "()L"+elementBuilderClassName+";", false);
                codegen.v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, elementBuilderClassName, "getRenderResults", "()Ljava/util/List;", false);
                codegen.v.visitVarInsn(Opcodes.ALOAD, 1);
                codegen.v.visitVarInsn(Opcodes.ILOAD, 2);
                codegen.v.visitVarInsn(Opcodes.ILOAD, 3);
                val r4aClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/R4a"
                codegen.v.visitMethodInsn(Opcodes.INVOKESTATIC, r4aClassName, "renderIntoWrapperView", "(Ljava/util/List;Ljava/util/List;Landroid/view/ViewGroup;II)V", false);
                codegen.v.visitVarInsn(Opcodes.ALOAD, 0);
                codegen.v.visitVarInsn(Opcodes.ALOAD, 0);
                codegen.v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "getMarkupBuilder", "()L"+elementBuilderClassName+";", false);
                codegen.v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, elementBuilderClassName, "getRenderResults", "()Ljava/util/List;", false);
                codegen.v.visitInsn(Opcodes.DUP);
                codegen.v.visitLdcInsn("R4a.renderResults");
                codegen.v.visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                codegen.v.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, "elements", "Ljava/util/List;");
                codegen.v.visitInsn(Opcodes.RETURN);
            }
        })

        for(fieldDescriptor in descriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
            var isAttribute = false;
            for(annotation in fieldDescriptor.annotations) {
                if("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Attribute" == annotation.type.constructor.declarationDescriptor!!.fqNameSafe.toString()) isAttribute = true
            }
            if(isAttribute) {
                fieldDescriptor as PropertyDescriptorImpl
                    val unitType : SimpleType = descriptor.builtIns.unitType
                    val methodName = "\$\$set"+fieldDescriptor.name.toString().substring(0,1).toUpperCase()+fieldDescriptor.name.toString().substring(1)
                    val newMethod = SimpleFunctionDescriptorImpl.create(descriptor, Annotations.EMPTY, Name.identifier(methodName), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)

                    val parameter = ValueParameterDescriptorImpl(
                            newMethod,
                            null, 0, Annotations.EMPTY,
                            Name.identifier(fieldDescriptor.name.toString()), // Ensure name is simple identifier (not fully qualified)
                            fieldDescriptor.type,
                            false,
                            false,
                            false, null, SourceElement.NO_SOURCE)
                    newMethod.initialize(null, descriptor.thisAsReceiverParameter, emptyList(), listOf(parameter), unitType, Modality.FINAL, Visibilities.PUBLIC)

                val codegen = this
                newMethod.write(this) {

                    var fieldName = fieldDescriptor.name.toString()
                    var fieldType = codegen.typeMapper.mapType(fieldDescriptor.type).internalName

                    v.visitVarInsn(Opcodes.ALOAD, 0);
                    v.visitVarInsn(Opcodes.ALOAD, 1);
                    v.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, fieldName, "L"+fieldType+";");

                    v.visitInsn(Opcodes.RETURN)
                }
            }
        }
    }

    private fun writeCreatorClass(codegen: ImplementationBodyCodegen, classDescriptor: GeneratedViewClassDescriptor) {
        val asmType = codegen.typeMapper.mapType(classDescriptor.defaultType)
        val classBuilderForCreator = codegen.state.factory.newVisitor(JvmDeclarationOrigin.NO_ORIGIN, Type.getObjectType(asmType.internalName), codegen.myClass.containingKtFile)
        val classContextForCreator = ClassContext(codegen.typeMapper, classDescriptor, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = MyCodegen(codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        val rerenderableClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/Rerenderable"
        classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, asmType.internalName, null, "android/widget/LinearLayout", arrayOf(rerenderableClassName))

        val componentType = codegen.typeMapper.mapType(codegen.descriptor)
        writeConstructor(componentType, codegenForCreator, classDescriptor)
        writeRerenderMethod(codegen.descriptor, codegenForCreator, classDescriptor)
        writeSetterMethod(componentType, codegenForCreator, classDescriptor)
        classBuilderForCreator.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_SYNTHETIC, "componentInstance", "L"+componentType.internalName+";", null, null)

        codegenForCreator.generateKotlinMetadataAnnotation()

        classBuilderForCreator.done()
    }

    private fun writeConstructor(componentType: Type, codegen: ImplementationBodyCodegen, syntheticWrapperViewClassDescriptor: GeneratedViewClassDescriptor) {
        syntheticWrapperViewClassDescriptor.unsubstitutedPrimaryConstructor!!.write(codegen) {

            val internalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;

            v.load(0, codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType))
            v.load(1, Type.getObjectType("android/widget/LinearLayout"))
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "android/widget/LinearLayout", "<init>", "(Landroid/content/Context;)V", false);


            v.visitVarInsn(Opcodes.ALOAD, 0)
            v.visitTypeInsn(Opcodes.NEW, componentType.internalName)
            v.visitInsn(Opcodes.DUP)
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, componentType.internalName, "<init>", "()V", false)
            v.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, "componentInstance", "L"+componentType.internalName+";")
            v.visitVarInsn(Opcodes.ALOAD, 0)
            val componentClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/Component"
            val rerenderableClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m/google/r4a/Rerenderable"
            v.visitMethodInsn(Opcodes.INVOKESTATIC, componentClassName, "addWrapper", "(L"+rerenderableClassName+";)V", false);


            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitTypeInsn(Opcodes.NEW, "android/widget/LinearLayout\$LayoutParams");
            v.visitInsn(Opcodes.DUP);
            v.visitInsn(Opcodes.ICONST_M1);
            v.visitIntInsn(Opcodes.BIPUSH, -2);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "android/widget/LinearLayout\$LayoutParams", "<init>", "(II)V", false);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "setLayoutParams", "(Landroid/view/ViewGroup\$LayoutParams;)V", false);
            v.areturn(Type.VOID_TYPE)
        }


        // Override onAttachedToWindow() to invoke rerender
        // We can't rerender in the constructor because the component's required fields will not have been initialized (via setters on the WrapperView)
        val unitType : SimpleType = syntheticWrapperViewClassDescriptor.builtIns.unitType
        val newMethod = SimpleFunctionDescriptorImpl.create(syntheticWrapperViewClassDescriptor, Annotations.EMPTY, Name.identifier("onAttachedToWindow"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
        newMethod.initialize(null, syntheticWrapperViewClassDescriptor.thisAsReceiverParameter, emptyList(), emptyList(), unitType, Modality.FINAL, Visibilities.PUBLIC)
        newMethod.write(codegen) {
            val internalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;
            v.load(0, codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType))
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "flushComponentRerender", "()V", false);
            v.load(0, codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType))
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "android/widget/LinearLayout", "onAttachedToWindow", "()V", false);
            v.visitInsn(Opcodes.RETURN);
        }
    }

    private fun writeRerenderHelperConstructor(componentType: Type, codegen: ImplementationBodyCodegen, syntheticRerenderHelperClassDescriptor: GeneratedRerenderHelperClassDescriptor) {
        syntheticRerenderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!.write(codegen) {

            val internalClassName = codegen.typeMapper.mapType(syntheticRerenderHelperClassDescriptor.defaultType).internalName;
            val parameterType = codegen.typeMapper.mapType(syntheticRerenderHelperClassDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters[0].type)

            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitVarInsn(Opcodes.ALOAD, 1);
            v.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, "view", "L"+parameterType.internalName+";");
            v.areturn(Type.VOID_TYPE)
        }
    }

    private fun writeRerenderMethod(componentClassDescriptor: ClassDescriptor, codegen: ImplementationBodyCodegen, syntheticWrapperViewClassDescriptor: GeneratedViewClassDescriptor) {
        val renderHelperClassDescriptor = GeneratedRerenderHelperClassDescriptor(syntheticWrapperViewClassDescriptor.fqNameSafe.shortName().identifier+"\$RenderHelper", syntheticWrapperViewClassDescriptor.containingDeclaration, syntheticWrapperViewClassDescriptor)
        writeRerenderHelperClass(codegen, renderHelperClassDescriptor)

        syntheticWrapperViewClassDescriptor.getFlushMethodDescriptor().write(codegen) {
            val componentType = codegen.typeMapper.mapType(componentClassDescriptor.defaultType);
            val internalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;
            val rerenderHelperClassName = codegen.typeMapper.mapType(renderHelperClassDescriptor.defaultType).internalName;
            /*
                public void rerender() {
                    try {
                        componentInstance.renderIntoViewGroup(this, 0, this.getChildCount());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
             */

            val l0 = Label()
            val l1 = Label()
            val l2 = Label()
            v.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception")
            v.visitLabel(l0)
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, internalClassName, "componentInstance", "L"+componentType.internalName+";");
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitInsn(Opcodes.ICONST_0);
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "getChildCount", "()I", false);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, componentType.internalName, "renderIntoViewGroup", "(Landroid/view/ViewGroup;II)V", false);
            v.visitLabel(l1)
            val l4 = Label()
            v.visitJumpInsn(Opcodes.GOTO, l4)
            v.visitLabel(l2)
            v.visitVarInsn(Opcodes.ASTORE, 1)
            v.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException")
            v.visitInsn(Opcodes.DUP)
            v.visitVarInsn(Opcodes.ALOAD, 1)
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false)
            v.visitInsn(Opcodes.ATHROW)
            v.visitLabel(l4)
            v.visitInsn(Opcodes.RETURN)
        }

        syntheticWrapperViewClassDescriptor.getRerenderMethodDescriptor().write(codegen) {

            val componentType = codegen.typeMapper.mapType(componentClassDescriptor.defaultType);
            val internalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;
            val rerenderHelperClassName = codegen.typeMapper.mapType(renderHelperClassDescriptor.defaultType).internalName;

            // new Handler(Looper.getMainLooper()).post(new MyWrapperView$RerenderHelper(this, this.componentInstance));

            v.visitTypeInsn(Opcodes.NEW, "android/os/Handler");
            v.visitInsn(Opcodes.DUP);
            v.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/Looper", "getMainLooper", "()Landroid/os/Looper;", false);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, "android/os/Handler", "<init>", "(Landroid/os/Looper;)V", false);
            v.visitTypeInsn(Opcodes.NEW, rerenderHelperClassName);
            v.visitInsn(Opcodes.DUP);
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, rerenderHelperClassName, "<init>", "(L"+internalClassName+";)V", false);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/os/Handler", "post", "(Ljava/lang/Runnable;)Z", false);
            v.visitInsn(Opcodes.POP);
            v.visitInsn(Opcodes.RETURN);
        }
    }

    private fun writeRunMethod(componentType: Type, codegen: ImplementationBodyCodegen, syntheticWrapperViewClassDescriptor: GeneratedRerenderHelperClassDescriptor) {
        val parameterType = syntheticWrapperViewClassDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters[0].type.asmType(codegen.typeMapper);
        syntheticWrapperViewClassDescriptor.getRunMethodDescriptor().write(codegen) {
            val internalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, internalClassName, "view", "L"+parameterType.internalName+";");
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, parameterType.internalName, "flushComponentRerender", "()V", false);
            v.visitInsn(Opcodes.RETURN);
        }
    }

    private fun writeRerenderHelperClass(codegen: ImplementationBodyCodegen, classDescriptor: GeneratedRerenderHelperClassDescriptor) {
        val asmType = codegen.typeMapper.mapType(classDescriptor.defaultType)
        val classBuilderForCreator = codegen.state.factory.newVisitor(JvmDeclarationOrigin.NO_ORIGIN, Type.getObjectType(asmType.internalName), codegen.myClass.containingKtFile)
        val classContextForCreator = ClassContext(codegen.typeMapper, classDescriptor, OwnerKind.IMPLEMENTATION, codegen.context.parentContext, null)
        val codegenForCreator = MyCodegen(codegen.myClass, classContextForCreator, classBuilderForCreator, codegen.state, codegen.parentCodegen, false)

        classBuilderForCreator.defineClass(null, Opcodes.V1_6, Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, asmType.internalName, null, "java/lang/Object", arrayOf("java/lang/Runnable"))

        val componentType = codegen.typeMapper.mapType(codegen.descriptor)
        writeRerenderHelperConstructor(componentType, codegenForCreator, classDescriptor)
        writeRunMethod(componentType, codegenForCreator, classDescriptor)
        val viewType = classDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters[0].type.asmType(codegen.typeMapper);
        classBuilderForCreator.newField(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_SYNTHETIC, "view", "L"+viewType.internalName+";", null, null)
        codegenForCreator.generateKotlinMetadataAnnotation()

        classBuilderForCreator.done()
    }

    private fun writeSetterMethod(componentType: Type, codegen: ImplementationBodyCodegen, syntheticWrapperViewClassDescriptor: GeneratedViewClassDescriptor) {
        for(descriptor in syntheticWrapperViewClassDescriptor.getSetterMethodDescriptors()) {
            descriptor.write(codegen) {

                val wrapperViewInternalClassName = codegen.typeMapper.mapType(syntheticWrapperViewClassDescriptor.defaultType).internalName;
                val componentInternalClassName = componentType.internalName;
                var fieldName = descriptor.name.identifier.substring(3, 4).toLowerCase()+descriptor.name.identifier.substring(4)
                var fieldType = codegen.typeMapper.mapType(descriptor.valueParameters[0].type).internalName

                /*
                TODO: save field on wrapper object
                v.visitVarInsn(Opcodes.ALOAD, 0);
                v.visitVarInsn(Opcodes.ALOAD, 1);
                v.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, fieldName, "L"+fieldType+";");
                */

                v.visitVarInsn(Opcodes.ALOAD, 0);
                v.visitFieldInsn(Opcodes.GETFIELD, wrapperViewInternalClassName, "componentInstance", "L"+componentInternalClassName+";");
                v.visitVarInsn(Opcodes.ALOAD, 1);
                v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, componentInternalClassName, "\$\$"+descriptor.name.identifier, "(L"+fieldType+";)V", false);

                v.visitVarInsn(Opcodes.ALOAD, 0);
                v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapperViewInternalClassName, "rerender", "()V", false);

                v.visitInsn(Opcodes.RETURN)
            }
        }
    }

    private fun FunctionDescriptor.write(codegen: ImplementationBodyCodegen, code: ExpressionCodegen.() -> Unit) {
        codegen.functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, this, object : CodegenBased(codegen.state) {
            override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) {
                e.code()
            }
        })
    }
}

private class DefaultClassConstructorDescriptor(containingClass: ClassDescriptor, source: SourceElement) : ClassConstructorDescriptorImpl(containingClass, null, Annotations.EMPTY, true, CallableMemberDescriptor.Kind.DECLARATION, source) {
    init {
        initialize(emptyList<ValueParameterDescriptor>(),
                   getDefaultConstructorVisibility(containingClass))
    }
}
