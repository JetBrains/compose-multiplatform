package org.jetbrains.kotlin.r4a

import com.intellij.util.SmartList
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.collectAllFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit


object R4aUtils {

    private val realGetterSetterCache = object : SLRUCache<DeclarationDescriptor, Collection<AttributeInfo>>(10, 10) {
        override fun createValue(descriptor: DeclarationDescriptor?): Collection<AttributeInfo> {
            if (descriptor == null) return listOf()
            return when (descriptor) {
                is ClassDescriptor -> {
                    descriptor
                        .unsubstitutedMemberScope
                        .getContributedDescriptors()
                        .mapNotNull { d ->
                            when (d) {
                                is FunctionDescriptor -> {
                                    if (d.overriddenDescriptors.isNotEmpty()) null
                                    else if (d.valueParameters.size != 1) null
                                    else if (d.returnType?.isUnit() != true) null // only void setters are allowed
                                    else if (!isSetterMethodName(d.name.identifier)) null
                                    else AttributeInfo(
                                        name = propertyNameFromSetterMethod(d.name.identifier),
                                        descriptor = d,
                                        type = d.valueParameters[0].type,
                                        // For now, we just assume that setter methods are not required attributes...
                                        // In the future, we should provide some sort of "@Required" annotation so that we can check to see if
                                        // that exists, and then mark it as required in that case.
                                        required = false
                                    )
                                }
                                is PropertyDescriptor -> {
                                    if (d.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE || d.overriddenDescriptors.isNotEmpty()) null
                                    else AttributeInfo(
                                        name = d.name.identifier,
                                        descriptor = d,
                                        type = d.type,
                                        required = when {
                                        // nullable types are never required. If the attribute is omitted, we can provide null by default
                                            d.type.isMarkedNullable -> false

                                        // if there is a compile-time initializer, then its not required
                                            d.compileTimeInitializer != null -> false

                                        // if the property has an initializer expression, then its not required
                                            (d.source.getPsi() as? KtProperty)?.initializer != null -> false

                                        // otherwise, assume its required!
                                            else -> true
                                        }
                                    )
                                }
                                else -> null
                            }
                        }
                }
                is FunctionDescriptor -> descriptor.valueParameters.map { param ->
                    AttributeInfo(
                        name = param.name.identifier,
                        descriptor = param,
                        type = param.type,
                        required = !param.hasDefaultValue()
                    )
                }
                else -> listOf()
            }
        }
    }

    private val adapterSetterCache = object : SLRUCache<ClassDescriptor, Collection<AttributeInfo>>(10, 10) {
        override fun createValue(cd: ClassDescriptor?): Collection<AttributeInfo> {
            if (cd == null) return listOf()
            return cd
                .unsubstitutedMemberScope
                .getContributedDescriptors()
                .mapNotNull { it as? FunctionDescriptor }
                .mapNotNull { fn ->
                    if (fn.valueParameters.size != 2) null
                    else if (fn.returnType?.isUnit() != true) null
                    else if (!isSetterMethodName(fn.name.identifier)) null
                    else AttributeInfo(
                        name = propertyNameFromSetterMethod(fn.name.identifier),
                        descriptor = fn,
                        type = fn.valueParameters[1].type,
                        required = false
                    )
                }
        }
    }

    data class AttributeInfo(
        val name: String,
        val descriptor: DeclarationDescriptorWithVisibility,
        val type: KotlinType,
        val required: Boolean
    )

    fun generateR4APackageName() = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a"

    fun r4aFqName(cname: String) = FqName("${generateR4APackageName()}.$cname")

    fun setterMethodFromPropertyName(name: String): String {
        return "set${name[0].toUpperCase()}${name.slice(1 until name.length)}"
    }

    fun propertyNameFromSetterMethod(name: String): String {
        return if (name.startsWith("set")) "${name[3].toLowerCase()}${name.slice(4 until name.length)}" else name
    }

    private fun isSetterMethodName(name: String): Boolean {
        return name.startsWith("set") && name.length > 3 && !name[3].isLowerCase() // use !lower to capture non-alpha chars
    }

    private fun getSyntheticDescriptors(types: Collection<KotlinType>, facade: ResolutionFacade?): Collection<DeclarationDescriptor> {
        val scope = facade?.getFrontendService(SyntheticScopes::class.java) ?: return listOf()
        return scope.collectSyntheticMemberFunctions(types) + scope.collectSyntheticExtensionProperties(types)
    }

    fun getPossibleAttributesForDescriptor(
        descriptor: DeclarationDescriptor,
        scope: LexicalScope,
        facade: ResolutionFacade?
    ): Collection<AttributeInfo> {
        val module = scope.ownerDescriptor.module
        val componentClass = module.findClassAcrossModuleDependencies(ClassId.topLevel(r4aFqName("Component"))) ?: error("Could not find component")
        val adapterDescriptor = module.findClassAcrossModuleDependencies(ClassId.topLevel(r4aFqName("AttributeAdapter"))) ?: return listOf()
        return when (descriptor) {
            is ClassDescriptor -> {
                var cd: ClassDescriptor? = descriptor
                val result = mutableListOf<AttributeInfo>()

                while (cd != null && cd != componentClass) {
                    val validAttributes = realGetterSetterCache[cd].filter { d ->
                        Visibilities.isVisibleIgnoringReceiver(d.descriptor, scope.ownerDescriptor)
                    }
                    result.addAll(validAttributes)
                    cd = cd.getSuperClassNotAny()
                }


                val allDescriptorsInScope = scope
                    .collectAllFromMeAndParent { s -> s.getContributedDescriptors() }

                for (d in allDescriptorsInScope) {
                    when (d) {
                        is ClassDescriptor -> {
                            if (d.defaultType.isSubtypeOf(adapterDescriptor.defaultType)) {
                                val validAttributes = adapterSetterCache[d].filter {
                                    val fn = it.descriptor as? FunctionDescriptor ?: return@filter false
                                    Visibilities.isVisibleIgnoringReceiver(fn, scope.ownerDescriptor) &&
                                            descriptor.defaultType.isSubtypeOf(fn.valueParameters[0].type)
                                }
                                result.addAll(validAttributes)
                            }
                        }
                    // extension properties
                        is PropertyDescriptor -> {
                            val receiverParam = d.extensionReceiverParameter
                            if (
                                (receiverParam != null) &&
                                (descriptor.defaultType.isSubtypeOf(receiverParam.value.type)) &&
                                (Visibilities.isVisibleIgnoringReceiver(d, scope.ownerDescriptor))
                            ) {
                                result.add(
                                    AttributeInfo(
                                        name = d.name.identifier,
                                        descriptor = d,
                                        type = d.type,
                                        required = false
                                    )
                                )
                            }
                        }
                    // extension functions
                        is FunctionDescriptor -> {
                            val receiverParam = d.extensionReceiverParameter
                            if (
                                (receiverParam != null) &&
                                (d.valueParameters.size == 1) &&
                                (d.returnType?.isUnit() != false) &&
                                (isSetterMethodName(d.name.identifier)) &&
                                (Visibilities.isVisibleIgnoringReceiver(d, scope.ownerDescriptor)) &&
                                (descriptor.defaultType.isSubtypeOf(receiverParam.value.type))
                            ) {
                                result.add(
                                    AttributeInfo(
                                        name = propertyNameFromSetterMethod(d.name.identifier),
                                        descriptor = d,
                                        type = d.valueParameters[0].type,
                                        required = false
                                    )
                                )
                            }
                        }
                    }

                }

                val syntheticGettersSetters = getSyntheticDescriptors(listOf(descriptor.defaultType), facade)
                    .mapNotNull { d ->
                        when (d) {
                        // only extension functions exist right now, but in the future, properties might...
                            is SamAdapterExtensionFunctionDescriptor -> {
                                if (d.returnType?.isUnit() != true) null // only void setters are allowed
                                else if (!isSetterMethodName(d.name.identifier)) null
                                else if (d.valueParameters.size != 1) null
                                else if (!Visibilities.isVisibleIgnoringReceiver(d, scope.ownerDescriptor)) null
                                else AttributeInfo(
                                    name = propertyNameFromSetterMethod(d.name.identifier),
                                    descriptor = d,
                                    type = d.valueParameters[0].type,
                                    // For now, we just assume that setter methods are not required attributes...
                                    // In the future, we should provide some sort of "@Required" annotation so that we can check to see if
                                    // that exists, and then mark it as required in that case.
                                    required = false
                                )
                            }
                            else -> null
                        }
                    }
                result.addAll(syntheticGettersSetters)

                result
            }
            is FunctionDescriptor -> realGetterSetterCache[descriptor]
            else -> listOf()
        }
    }

    private fun storeSimpleNameExpression(
        expression: KtSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)
        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, expression, trace)

        val qualifier = when (descriptor) {
            is PackageViewDescriptor -> PackageQualifier(expression, descriptor)
            is ClassDescriptor -> ClassQualifier(expression, descriptor)
            is TypeParameterDescriptor -> TypeParameterQualifier(expression, descriptor)
            is TypeAliasDescriptor -> descriptor.classDescriptor?.let {
                TypeAliasQualifier(expression, descriptor, it)
            }
            else -> null
        }

        if (qualifier != null) {
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }

    private fun KtExpression.asQualifierPartList(): List<QualifiedExpressionResolver.QualifierPart> {
        val result = SmartList<QualifiedExpressionResolver.QualifierPart>()

        fun addQualifierPart(expression: KtExpression?): Boolean {
            if (expression is KtSimpleNameExpression) {
                result.add(QualifiedExpressionResolver.QualifierPart(expression))
                return true
            }
            return false
        }

        var expression: KtExpression? = this
        while (true) {
            if (addQualifierPart(expression)) break
            if (expression !is KtQualifiedExpression) break

            addQualifierPart(expression.selectorExpression)

            expression = expression.receiverExpression
        }

        return result.asReversed()
    }

    fun resolveDeclaration(
        expression: KtExpression,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace?,
        scopeForFirstPart: LexicalScope
    ): DeclarationDescriptor? {
        val path = expression.asQualifierPartList()
        val firstPart = path.first()
        var currentDescriptor: DeclarationDescriptor? = null
        currentDescriptor = currentDescriptor ?: scopeForFirstPart.findVariable(firstPart.name, firstPart.location)
        currentDescriptor = currentDescriptor ?: scopeForFirstPart.findFunction(firstPart.name, firstPart.location)
        currentDescriptor = currentDescriptor ?: scopeForFirstPart.findClassifier(firstPart.name, firstPart.location)
        currentDescriptor = currentDescriptor ?:
                moduleDescriptor.getPackage(FqName.topLevel(firstPart.name)).let { if (it.isEmpty()) null else it }

        if (currentDescriptor == null) {
            trace?.report(Errors.UNRESOLVED_REFERENCE.on(firstPart.expression, firstPart.expression))
            return null
        } else if (trace != null) {
            storeSimpleNameExpression(firstPart.expression, currentDescriptor, trace)
        }

        // TODO(lmr): we need to add visibility checks into this function...

        for (qualifierPartIndex in 1 until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                // TODO(lmr): i wonder if we could allow this for Ktx. Seems like a nice to have
                    is TypeAliasDescriptor -> // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                        null
                    is ClassDescriptor -> {
                        var next: DeclarationDescriptor? = null
                        next = next ?: currentDescriptor.unsubstitutedInnerClassesScope.getContributedClassifier(
                            qualifierPart.name,
                            qualifierPart.location
                        )
                        next = next ?: currentDescriptor.unsubstitutedInnerClassesScope.getContributedFunctions(
                            qualifierPart.name,
                            qualifierPart.location
                        ).singleOrNull()
                        if (currentDescriptor.kind == ClassKind.OBJECT) {
                            next = next ?: currentDescriptor.unsubstitutedMemberScope.getContributedFunctions(
                                qualifierPart.name,
                                qualifierPart.location
                            ).singleOrNull()
                        }
                        val cod = currentDescriptor.companionObjectDescriptor
                        if (cod != null) {
                            next = next ?: cod.unsubstitutedMemberScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)
                            next = next ?: cod.unsubstitutedMemberScope.getContributedFunctions(
                                qualifierPart.name,
                                qualifierPart.location
                            ).singleOrNull()
                            next = next ?: cod.unsubstitutedMemberScope.getContributedVariables(
                                qualifierPart.name,
                                qualifierPart.location
                            ).singleOrNull()
                        }
                        next = next ?: currentDescriptor.staticScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)
                        next = next ?: currentDescriptor.staticScope.getContributedFunctions(
                            qualifierPart.name,
                            qualifierPart.location
                        ).singleOrNull()
                        next = next ?: currentDescriptor.staticScope.getContributedVariables(
                            qualifierPart.name,
                            qualifierPart.location
                        ).singleOrNull()
                        next
                    }
                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.getContributedClassifier(qualifierPart.name, qualifierPart.location)
                        }
                    }
                    else ->
                        null
                }

            if (nextPackageOrClassDescriptor == null) {
                trace?.report(Errors.UNRESOLVED_REFERENCE.on(qualifierPart.expression, qualifierPart.expression))
                return null
            } else if (trace != null) {
                storeSimpleNameExpression(qualifierPart.expression, nextPackageOrClassDescriptor, trace)
            }

            currentDescriptor = nextPackageOrClassDescriptor
        }

        return currentDescriptor
    }
}