/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.idea

import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.util.getResolveScope
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.inspections.collections.isFunctionOfAnyKind
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isChildOf
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.kotlin.ResolvedKtxElementCall
import androidx.compose.plugins.kotlin.hasHiddenAttributeAnnotation
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes

data class AttributeInfo(
    val name: String,
    val type: KotlinType
) {
    var isImmediate: Boolean = false
    var isRequired: Boolean = false
    var isExtension: Boolean = false
    var isChildren: Boolean = false
    var isPivotal: Boolean = false
    var isImported: Boolean = false
    var descriptor: DeclarationDescriptor? = null
    var contributingDescriptor: ClassifierDescriptor? = null
    var extensionType: KotlinType? = null
}

class AttributeInfoExtractor(
    file: KtFile,
    // TODO(jdemeulenaere): Remove those params and try to share more code instead.
    private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    declarationTranslator: (KtDeclaration) -> KtDeclaration? = { it },
    private val ktxCall: ResolvedKtxElementCall? = null
) {
    private val module = file.findModuleDescriptor()
    private val composeComponentDescriptor =
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(ComposeUtils.composeFqName("Component"))
        )
    private val androidViewDescriptor =
        module.findClassAcrossModuleDependencies(
            ClassId.topLevel(FqName("android.view.View"))
        )
    private val indicesHelper = KotlinIndicesHelper(
        file.getResolutionFacade(),
        getResolveScope(file),
        visibilityFilter,
        declarationTranslator,
        file = file
    )

    private val ktxCallResolvedCalls by lazy { ktxCall?.emitOrCall?.resolvedCalls() ?: emptyList() }
    private val referrableDescriptors by lazy {
        ktxCallResolvedCalls
            .map {
                val resultingDescriptor = it.resultingDescriptor
                val result: DeclarationDescriptor = when {
                    it is VariableAsFunctionResolvedCall -> it.variableCall.candidateDescriptor
                    resultingDescriptor is ConstructorDescriptor ->
                        resultingDescriptor.constructedClass
                    else -> resultingDescriptor
                }
                result
            }
    }

    private val instanceTypes by lazy {
        ktxCallResolvedCalls
            .mapNotNull { it.resultingDescriptor.returnType }
            .filter { !it.isUnit() }
    }

    private fun isDescriptorReferredTo(descriptor: DeclarationDescriptor?): Boolean {
        return descriptor in referrableDescriptors
    }

    fun extract(tagDescriptor: DeclarationDescriptor, receiver: (Sequence<AttributeInfo>) -> Unit) {
        when (tagDescriptor) {
            is ClassDescriptor -> {
                receiver(
                    tagDescriptor
                        .constructors
                        .filter { visibilityFilter(it) }
                        .flatMap { it.valueParameters }
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                )

                receiver(
                    tagDescriptor
                        .unsubstitutedMemberScope
                        .getContributedDescriptors()
                        .filter { it.isValidAttribute() }
                        .filter { visibilityFilter(it) }
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                )

                val defaultType = tagDescriptor.defaultType
                receiver(
                    indicesHelper
                        .getCallableTopLevelExtensions(
                            callTypeAndReceiver = CallTypeAndReceiver.DEFAULT,
                            receiverTypes = listOf(defaultType),
                            nameFilter = { true },
                            declarationFilter = { true }
                        )
                        .filter { it.isValidAttribute() }
                        // We don't `.filter { visibilityFilter(it) }` as KotlinIndicesHelper was instantiated with that filter already.
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                )
            }
            is FunctionDescriptor -> {
                receiver(
                    tagDescriptor
                        .valueParameters
                        .asSequence()
                        .mapNotNull { it.constructAttributeInfo(isImported = true) }
                )
            }
            is VariableDescriptor -> {
                if (tagDescriptor.type.isFunctionOfAnyKind()) {
                    receiver(
                        tagDescriptor.type
                            .getValueParameterTypesFromFunctionType()
                            .asSequence()
                            .mapNotNull { it.constructAttributeInfo() }
                    )
                }
            }
        }
    }

    private fun DeclarationDescriptor.isValidAttribute(): Boolean {
        return when (this) {
            is FunctionDescriptor -> {
                when {
                    valueParameters.size != 1 -> false
                    returnType?.isUnit() != true -> false
                    // only void setters are allowed
                    else -> ComposeUtils.isSetterMethodName(name.identifier)
                }
            }
            is PropertyDescriptor -> isVar
            else -> false
        }
    }

    // TODO(jdemeulenaere): This kind of code is duplicated multiple times in different files. Extract and refactor in a single extension.
    private val composeChildrenAnnotationFqName = ComposeUtils.composeFqName("Children")
    private fun Annotated.hasChildrenAnnotation() =
        annotations.hasAnnotation(composeChildrenAnnotationFqName)

    private fun TypeProjection.constructAttributeInfo(): AttributeInfo? {
        return AttributeInfo(
            name = type.extractParameterNameFromFunctionTypeArgument()?.asString() ?: return null,
            type = type
        ).also {
            it.descriptor = null
            it.isChildren = type.hasChildrenAnnotation()
            it.isRequired = true
            it.isExtension = false
            it.isImmediate = true
            it.isImported = true
            it.isPivotal = false
            it.contributingDescriptor = null
            it.extensionType = null
        }
    }

    private fun DeclarationDescriptor.constructAttributeInfo(isImported: Boolean): AttributeInfo? {
        if (shouldFilter(this)) return null

        return AttributeInfo(
            name = when (this) {
                is FunctionDescriptor -> ComposeUtils.propertyNameFromSetterMethod(name.asString())
                else -> name.asString()
            },
            type = when (this) {
                is FunctionDescriptor -> valueParameters.firstOrNull()?.type ?: return null
                is PropertyDescriptor -> type
                is ValueParameterDescriptor -> type
                else -> return null
            }
        ).also {
            it.descriptor = this
            it.isChildren = hasChildrenAnnotation()
            it.isRequired = when (this) {
                is PropertyDescriptor -> {
                    isLateInit || !isVar
                }
                is FunctionDescriptor -> false
                is ValueParameterDescriptor -> {
                    !hasDefaultValue()
                }
                else -> false
            }
            it.isExtension = isExtension
            it.isImmediate = when {
                this is CallableMemberDescriptor && kind ==
                        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> false
                isDescriptorReferredTo(containingDeclaration) -> true
                containingDeclaration is ConstructorDescriptor &&
                        isDescriptorReferredTo(containingDeclaration?.containingDeclaration) -> true
                else -> false
            }
            it.isImported = isImported
            it.isPivotal = when (this) {
                is PropertyDescriptor -> !isVar && !isExtension
                is ValueParameterDescriptor -> {
                    // TODO(lmr): decide if we still want/need this with new type resolution
                    false // in this case there is no component instance, so nothing is pivotal
                }
                else -> false
            }
            it.contributingDescriptor = when (this) {
                is FunctionDescriptor -> getContributingDescriptor(this)
                is PropertyDescriptor -> getContributingDescriptor(this)
                else -> null
            }
            it.extensionType = when (this) {
                is FunctionDescriptor -> getExtensionType(this)
                is PropertyDescriptor -> getExtensionType(this)
                else -> null
            }
        }
    }

    private fun getContributingDescriptor(d: CallableMemberDescriptor): ClassifierDescriptor? {
        return DescriptorUtils.unwrapFakeOverride(d).containingDeclaration as? ClassifierDescriptor
    }

    private fun getExtensionType(d: CallableDescriptor): KotlinType? {
        val receiverParameter = d.extensionReceiverParameter ?: return null

        if (receiverParameter.value.original.type.isTypeParameter()) {
            return receiverParameter.value.original.type.supertypes().firstOrNull()
        }
        return receiverParameter.type
    }

    private val kotlinxAndroidSyntheticFqname = FqName("kotlinx.android.synthetic")
    private fun shouldFilter(d: DeclarationDescriptor): Boolean {
        if (d.fqNameSafe.isChildOf(kotlinxAndroidSyntheticFqname)) return true
        if (d.hasHiddenAttributeAnnotation()) return true
        val name = d.name.asString()

        when (name) {
            "_firstFrameRecord",
            "setRecompose" -> {
                if (d is CallableMemberDescriptor) {
                    val realDescriptor = getContributingDescriptor(d)
                    if (realDescriptor != null && realDescriptor != d &&
                        realDescriptor == composeComponentDescriptor) {
                        return true
                    }
                }
            }
        }

        // NOTE(lmr): View subclasses all have constructors with these params. We handle view creation somewhat automatically, and
        // we don't want these params to show up in the autocomplete despite people technically being able to use them. This is
        // kind of a gross way of removing them but seems to be a reasonably effective strategy.
        when (name) {
            "context",
            "attrs",
            "defStyle",
            "defStyleRes",
            "defStyleAttr" -> {
                if (
                    d is ValueParameterDescriptor &&
                    d.containingDeclaration is ConstructorDescriptor
                ) {
                    val cd = d.containingDeclaration.containingDeclaration as? ClassDescriptor
                    if (
                        cd != null &&
                        androidViewDescriptor != null &&
                        cd.isSubclassOf(androidViewDescriptor)
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }
}