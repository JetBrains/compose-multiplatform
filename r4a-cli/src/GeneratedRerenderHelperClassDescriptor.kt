package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.Printer

open class GeneratedRerenderHelperClassDescriptor: ClassDescriptor {
    private val containingDeclaration: DeclarationDescriptor
    private val name: Name
    private val wrapperClassDescriptor: GeneratedViewClassDescriptor

    constructor(helperClassNameIdentifier: String, containingDeclaration: DeclarationDescriptor, wrapperClassDescriptor: GeneratedViewClassDescriptor) {
        name = Name.identifier(helperClassNameIdentifier)
        this.containingDeclaration = containingDeclaration
        this.wrapperClassDescriptor = wrapperClassDescriptor;
        val superType = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.lang.Runnable")))!!.defaultType
        initialize(listOf(superType))
    }

    override fun isInline(): Boolean = false

    override fun getName() = name

    private val modality = Modality.FINAL
    private val kind = ClassKind.CLASS
    private val sourceElement = SourceElement.NO_SOURCE
    override val annotations = Annotations.EMPTY
    private lateinit var typeConstructor: TypeConstructor
    private lateinit var supertypes: List<KotlinType>
    private lateinit var defaultType: SimpleType

    private val thisAsReceiverParameter = LazyClassReceiverParameterDescriptor(this)

    fun initialize(supertypes: List<KotlinType>) {
        this.supertypes = supertypes
        this.typeConstructor = ClassTypeConstructorImpl(this, emptyList(), supertypes, LockBasedStorageManager.NO_LOCKS)
        this.defaultType = TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    override fun isActual(): Boolean = false
    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getConstructors(): Collection<ClassConstructorDescriptor> = listOf(getUnsubstitutedPrimaryConstructor()!!)
    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration
    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getKind(): ClassKind = kind
    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

    fun genScope(): MemberScope {

        return object : MemberScope {

            override fun getClassifierNames(): Set<Name>? = null

            override fun getFunctionNames() : Set<Name> {
                return setOf(Name.identifier("setMySweetAttribute"));
            }

            override fun getVariableNames(): Set<Name> {
                return emptySet()
            }

            override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
                return null;
            }

            override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
                return emptyList()
            }

            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
            {
                return emptyList()
            }

            override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor>
            {
                if(name == Name.identifier("run")) return listOf(getRunMethodDescriptor())
                return emptyList()
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

    var primaryConstructor: ClassConstructorDescriptor? = null

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        if(primaryConstructor == null) primaryConstructor = generateUnsubstitutedPrimaryConstructor()
        return primaryConstructor!!
    }

    fun generateUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        val constructor = ClassConstructorDescriptorImpl.create(this, Annotations.EMPTY, false, SourceElement.NO_SOURCE)
        val viewgroupParameter = ValueParameterDescriptorImpl(
                constructor,
                null, 0, Annotations.EMPTY,
                Name.identifier("viewgroup"),
                wrapperClassDescriptor.defaultType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE)
         constructor.initialize(listOf(viewgroupParameter), Visibilities.PUBLIC)

        constructor.apply {
            returnType = containingDeclaration.defaultType
        }

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
    public override fun isExpect(): Boolean = false
    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun toString(): String =
            "GeneratedViewClassDescriptor($fqNameUnsafe)"

    fun getRunMethodDescriptor() : SimpleFunctionDescriptor {
        val newMethod = SimpleFunctionDescriptorImpl.create(this, annotations, Name.identifier("run"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
        newMethod.initialize(null, this.thisAsReceiverParameter, emptyList(), emptyList(), builtIns.unitType, Modality.FINAL, Visibilities.PUBLIC)
        return newMethod
    }
}
