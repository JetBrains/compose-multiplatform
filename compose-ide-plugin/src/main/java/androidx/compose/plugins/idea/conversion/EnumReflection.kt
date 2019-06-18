/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.plugins.idea.conversion

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.elements.KtLightPsiLiteral
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import androidx.compose.plugins.idea.AttributeInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.ArrayList

private val MAGIC_CONSTANT_ANNOTATIONS = listOf("IntDef", "LongDef", "StringDef").flatMap {
    listOf("android.annotation.$it", "android.support.annotation.$it")
}

fun AttributeInfo.getPossibleValues(project: Project): List<FqName> {
    return getPossibleValues(
        project,
        descriptor ?: return emptyList()
    )
}

internal fun getPossibleValues(project: Project, descriptor: DeclarationDescriptor): List<FqName> {
    val valueDescriptor: ValueDescriptor = when (descriptor) {
        is FunctionDescriptor -> descriptor.valueParameters.takeIf { it.size == 1 }?.first()
        is PropertyDescriptor -> descriptor
        is ValueParameterDescriptor -> descriptor
        else -> null
    } ?: return emptyList()

    if (valueDescriptor.type.isEnum()) {
        return getPossibleValuesFromEnum(valueDescriptor.type)
    }

    return getPossibleValuesFromAnnotations(
        project,
        valueDescriptor
    )
}

internal fun getPossibleValuesFromEnum(type: KotlinType): List<FqName> {
    return (type.constructor.declarationDescriptor as ClassDescriptor)
        .unsubstitutedMemberScope
        .getDescriptorsFiltered()
        .filter {
            it is EnumEntrySyntheticClassDescriptor ||
                    (it is LazyClassDescriptor && it.kind == ClassKind.ENUM_ENTRY)
        }
        .mapNotNull { it.fqNameOrNull() }
}

private fun getPossibleValuesFromAnnotations(
    project: Project,
    descriptor: DeclarationDescriptor
): List<FqName> {
    val fqNames = arrayListOf<FqName>()

    // When possible, use the overload taking PsiModifierListOwner as parameter as it has support for external annotations.
    val psiElement = descriptor.findPsi() ?: descriptor.original.findPsi()
    if (psiElement != null && psiElement is PsiModifierListOwner) {
        fillPossibleEnumValues(
            project,
            psiElement,
            fqNames,
            hashSetOf()
        )
    } else {
        val visited = hashSetOf<PsiClass>()
        descriptor.annotations.forEach {
            val annotationFqName = it.fqName?.asString() ?: return@forEach
            fillPossibleEnumValues(
                project,
                annotationFqName,
                fqNames,
                visited
            )
        }
    }

    return fqNames
}

private fun fillPossibleEnumValues(
    project: Project,
    annotationFqName: String,
    enumFqNames: ArrayList<FqName>,
    visited: MutableSet<PsiClass>
) {
    val annotationClass = JavaPsiFacade.getInstance(project).findClass(
        annotationFqName,
        project.allScope()
    ) ?: return
    if (!visited.add(annotationClass)) return

    // We use the navigationElement if possible such that we also get annotations with SOURCE retention.
    val annotated = (annotationClass.navigationElement as? PsiModifierListOwner) ?: annotationClass
    fillPossibleEnumValues(
        project,
        annotated,
        enumFqNames,
        visited
    )
}

private fun fillPossibleEnumValues(
    project: Project,
    annotated: PsiModifierListOwner,
    enumFqNames: ArrayList<FqName>,
    visited: MutableSet<PsiClass>
) {
    val annotations = AnnotationUtil.getAllAnnotations(
        annotated,
        /* inHierarchy= */ true,
        /* visited= */ null
    )
    for (annotation in annotations) {
        val qualifiedName = annotation.qualifiedName ?: continue
        if (qualifiedName in MAGIC_CONSTANT_ANNOTATIONS) {
            enumFqNames.addAll(
                getMagicConstantEnumValues(
                    annotation
                )
            )
        } else {
            fillPossibleEnumValues(
                project,
                qualifiedName,
                enumFqNames,
                visited
            )
        }
    }
}

private fun getMagicConstantEnumValues(annotation: PsiAnnotation): List<FqName> {
    val valueAttribute = annotation.findAttributeValue("value")
    val allowedValues = (valueAttribute as? PsiArrayInitializerMemberValue)?.initializers
        ?: PsiAnnotationMemberValue.EMPTY_ARRAY
    return allowedValues
        .map<Any, Array<out PsiReference>> {
            when (it) {
                is PsiReference -> arrayOf(it)
                // TODO(jdemeulenaere): Fix the case when the value is expressed as a KtDotQualifiedExpression (i.e. 'View.VISIBLE' instead
                // of 'VISIBLE').
                is KtLightPsiLiteral -> it.references
                else -> emptyArray()
            }
        }
        .mapNotNull {
                references -> references.firstNotNullResult { it.resolve()?.getKotlinFqName() }
        }
}