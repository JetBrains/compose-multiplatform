package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.Printer

open class GeneratedViewClassDescriptor(val metadata: ComponentMetadata): ClassDescriptor {
    private val module: ModuleDescriptor
    private val containingDeclaration: ClassDescriptor

    override fun isInline(): Boolean = false
    override fun getName() = Name.identifier((metadata.descriptor.findPsi() as KtClass).fqName!!.shortName().identifier + "WrapperView")

    override fun isExpect(): Boolean = false
    override fun isActual(): Boolean = false
    private val modality = Modality.FINAL
    private val kind = ClassKind.CLASS
    private val sourceElement = SourceElement.NO_SOURCE
    override val annotations = Annotations.EMPTY
    private lateinit var typeConstructor: TypeConstructor
    private lateinit var supertypes: Collection<KotlinType>
    private lateinit var defaultType: SimpleType
    private lateinit var declaredTypeParameters: List<TypeParameterDescriptor>
    private var unsubstitutedPrimaryConstructor: ClassConstructorDescriptor? = null


    val componentInstanceField by lazy {
        PropertyDescriptorImpl.create(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("componentInstance"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false).apply {
            setType(KotlinTypeFactory.simpleType(Annotations.EMPTY, metadata.descriptor.typeConstructor, emptyList<TypeProjection>(), true), emptyList<TypeParameterDescriptor>(), thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
            initialize(
                PropertyGetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initialize(metadata.descriptor.defaultType)
                },
                PropertySetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initializeDefault()
                }
            )
        }
    }

    val dirtyField by lazy {
        PropertyDescriptorImpl.create(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("dirty"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false).apply {
            setType(builtIns.booleanType, emptyList<TypeParameterDescriptor>(), thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
            initialize(
                PropertyGetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initialize(builtIns.booleanType)
                },
                PropertySetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initializeDefault()
                }
            )
        }
    }

    val onAttachDescriptor by lazy {
        val newMethod = SimpleFunctionDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            Name.identifier("onAttachedToWindow"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )

        newMethod.initialize(
            null,
            this.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            builtIns.unitType,
            Modality.OPEN,
            Visibilities.PUBLIC
        )
        newMethod
    }

    val onDetachDescriptor by lazy {
        val newMethod = SimpleFunctionDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            Name.identifier("onDetachedFromWindow"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )

        newMethod.initialize(
            null,
            this.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            builtIns.unitType,
            Modality.OPEN,
            Visibilities.PUBLIC
        )
        newMethod
    }

    init {
        this.module = metadata.descriptor.module
        val linearLayout = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.widget.LinearLayout")))!!.defaultType
        val viewTreeObserverDescriptor = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewTreeObserver")))!!
        val preDrawListener = (viewTreeObserverDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(Name.identifier("OnPreDrawListener"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor).defaultType

        initialize(emptyList(), listOf(linearLayout, preDrawListener))
        this.containingDeclaration = metadata.descriptor
    }

    val onPreDraw by lazy {
        val viewTreeObserverDescriptor = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewTreeObserver")))!!
        val preDrawListener = (viewTreeObserverDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(Name.identifier("OnPreDrawListener"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor)
        val overridenMethod = preDrawListener.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("onPreDraw"), NoLookupLocation.FROM_BACKEND).single()
        val newMethod = SimpleFunctionDescriptorImpl.create(
            this,
            Annotations.EMPTY,
            Name.identifier("onPreDraw"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )
        newMethod.setSingleOverridden(overridenMethod)

        newMethod.initialize(
            null,
            this.thisAsReceiverParameter,
            emptyList(),
            emptyList(),
            builtIns.booleanType,
            Modality.OPEN,
            Visibilities.PUBLIC
        )
        newMethod
    }

    val compositionContextField by lazy {
        val compositionContextDescriptor = module.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("CompositionContext")))!!
        PropertyDescriptorImpl.create(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("compositionContext"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false).apply {
            setType(compositionContextDescriptor.defaultType, emptyList<TypeParameterDescriptor>(), thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
            initialize(
                PropertyGetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initialize(compositionContextDescriptor.defaultType)
                },
                PropertySetterDescriptorImpl(this, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, false, false, false, CallableMemberDescriptor.Kind.SYNTHESIZED, null, SourceElement.NO_SOURCE).apply {
                    initializeDefault()
                }
            )
        }
    }


    private val thisAsReceiverParameter = LazyClassReceiverParameterDescriptor(this)

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>, supertypes: Collection<KotlinType>) {
        this.declaredTypeParameters = declaredTypeParameters
        this.supertypes = supertypes
        this.typeConstructor = ClassTypeConstructorImpl(this, declaredTypeParameters, supertypes, LockBasedStorageManager.NO_LOCKS)
        this.defaultType = TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getConstructors(): Collection<ClassConstructorDescriptor> = listOf(getUnsubstitutedPrimaryConstructor())
    override fun getContainingDeclaration() = containingDeclaration
    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = declaredTypeParameters
    override fun getKind(): ClassKind = kind
    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

    fun genScope(): MemberScope {

        return object : MemberScope {

            override fun getFunctionNames() : Set<Name> {
                return setterMethodDescriptors.map { it.name }.toSet()
            }

            override fun getVariableNames(): Set<Name> {
                return emptySet()
            }

            override fun getClassifierNames(): Set<Name>? = null

            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
                return null;
            }

            override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
                return emptyList()
            }

            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
                if(name.identifier == "componentInstance") {
                    return listOf(componentInstanceField)
                }
                if(name.identifier == "compositionContext") {
                    return listOf(compositionContextField)
                }
                if(name.identifier == "dirty") {
                    return listOf(dirtyField)
                }
                return emptyList()
            }

            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
                if (name.identifier == "onDetachedFromWindow") {
                    return listOf(onDetachDescriptor)
                }
                if (name.identifier == "onAttachedToWindow") {
                    return listOf(onAttachDescriptor)
                }
                if (name.identifier == "onPreDraw") {
                    return listOf(onPreDraw)
                }

                return setterMethodDescriptors.filter { name == it.fqNameSafe.shortName() }
            }

            override fun printScopeStructure(p: Printer) {
                p.println(this::class.java.simpleName)
            }
        }
    }

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope = genScope()
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = genScope()
    override fun getStaticScope(): MemberScope = genScope()
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = genScope()
    override fun getUnsubstitutedMemberScope(): MemberScope = genScope()

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor {
        unsubstitutedPrimaryConstructor?.let { return it }
        val constructor = ClassConstructorDescriptorImpl.create(
                this,
                Annotations.EMPTY,
                false,
                SourceElement.NO_SOURCE
        )
        val contextParameter = ValueParameterDescriptorImpl(
                constructor,
                null, 0, Annotations.EMPTY,
                Name.identifier("context"),
                containingDeclaration.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.content.Context")))!!.defaultType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
        constructor.initialize(
                listOf(contextParameter),
                Visibilities.PUBLIC
        )

        constructor.apply {
            returnType = containingDeclaration.defaultType
        }

        unsubstitutedPrimaryConstructor = constructor

        return constructor
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = error("Class $this can't be substituted")

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor = thisAsReceiverParameter

    override fun getModality(): Modality = modality
    override fun getOriginal(): ClassDescriptor = this
    override fun getVisibility(): Visibility = Visibilities.PUBLIC
    override fun getSource(): SourceElement = sourceElement
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getDefaultType(): SimpleType = defaultType

    override fun isCompanionObject(): Boolean = false
    override fun isData(): Boolean = false
    override fun isInner(): Boolean = false
    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun toString(): String = "GeneratedViewClassDescriptor($fqNameUnsafe)"

    val setterMethodDescriptors: Collection<SimpleFunctionDescriptor> by lazy {
        val descriptors = ArrayList<SimpleFunctionDescriptor>()
        for(property in metadata.getAttributeDescriptors()) {

            val unitType : SimpleType = this.builtIns.unitType
            val methodName = "set"+property.name.identifier.substring(0,1).toUpperCase()+property.name.identifier.substring(1)
            val newMethod = SimpleFunctionDescriptorImpl.create(this, annotations, Name.identifier(methodName), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)

            val parameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 0, Annotations.EMPTY,
                property.name,
                property.type,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
            newMethod.initialize(null, this.thisAsReceiverParameter, emptyList(), listOf(parameter), unitType, Modality.FINAL, Visibilities.PUBLIC)
            descriptors.add(newMethod);
        }

        descriptors
    }

    var instanceCreatorFunction: SimpleFunctionDescriptor? = null;
    fun getInstanceCreatorFunction(componentClassDescriptor: ClassDescriptor) : SimpleFunctionDescriptor {
        if(instanceCreatorFunction != null) return instanceCreatorFunction!!
        val returnType : SimpleType = this.defaultType
        val annotations = AnnotationsImpl(listOf(AnnotationDescriptorImpl(componentClassDescriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("kotlin.jvm.JvmStatic")))!!.defaultType, HashMap<Name, ConstantValue<*>>(), SourceElement.NO_SOURCE)))
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
        instanceCreatorFunction = newMethod
        return newMethod
    }
}
