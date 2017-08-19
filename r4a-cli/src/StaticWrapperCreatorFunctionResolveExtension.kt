package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.experimental.EmptyCoroutineContext.plus

class StaticWrapperCreatorFunctionResolveExtension() : SyntheticResolveExtension {

    companion object {
        fun generateTheFunction(componentClassDescriptor: ClassDescriptor, wrapperViewDescriptor: GeneratedViewClassDescriptor, annotated: Boolean) : SimpleFunctionDescriptor {
            val returnType : SimpleType = wrapperViewDescriptor.defaultType
            val annotations = if(annotated) AnnotationsImpl(listOf(AnnotationDescriptorImpl(componentClassDescriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.jvm.JvmStatic")))!!.defaultType, HashMap<Name, ConstantValue<*>>(), SourceElement.NO_SOURCE))) else Annotations.EMPTY
            val newMethod = SimpleFunctionDescriptorImpl.create(componentClassDescriptor, annotations, Name.identifier("createInstance"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
            val contextParameter = ValueParameterDescriptorImpl(
                    newMethod,
                    null, 0, Annotations.EMPTY,
                    Name.identifier("context"),
                    componentClassDescriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.content.Context")))!!.defaultType,
                    false,
                    false,
                    false, null, SourceElement.NO_SOURCE)
            newMethod.initialize(null, if(componentClassDescriptor.isCompanionObject) componentClassDescriptor.thisAsReceiverParameter else null, emptyList(), listOf(contextParameter), returnType, Modality.OPEN, Visibilities.PUBLIC)

            return newMethod
        }
    }

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        val cls = thisDescriptor.source.getPsi() as? KtClass ?: return null
        if(!cls.getSuperNames().contains("Component")) return null; // TODO: Check/resolve fully-qualified name.
        if(thisDescriptor.isCompanionObject) return null;
        if("Component" == thisDescriptor.getSuperClassNotAny()?.fqNameSafe?.shortName()?.identifier)
        {
            return Name.identifier("Companion2")
        }
        return null;
    }

    override fun generateSyntheticMethods(cls: ClassDescriptor, name: Name, context: LazyClassContext, fromSupertypes: List<SimpleFunctionDescriptor>, result: MutableCollection<SimpleFunctionDescriptor>) {

        if(name != Name.identifier("createInstance")) return

        val componentClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Component"

            val containingClass = cls.containingDeclaration as? ClassDescriptor ?: return
            if (containingClass.getSuperClassNotAny()?.fqNameSafe != FqName(componentClassName)) return;

        val wrapperView = context.trace.bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, containingClass.source.getPsi() as KtClass)
        result.add(wrapperView!!.getInstanceCreatorFunction(cls))
    }
}
