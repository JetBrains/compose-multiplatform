/*
 * Copyright (C) 2019 The Android Open Source Project
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

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentOfType
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.psi.psiUtil.containingClass

internal const val DESKTOP_PREVIEW_ANNOTATION_FQN =
    "androidx.compose.desktop.ui.tooling.preview.Preview"
internal const val COMPOSABLE_FQ_NAME = "androidx.compose.runtime.Composable"

private val ComposableAnnotationClassId = ClassId.topLevel(FqName(COMPOSABLE_FQ_NAME))
private val DesktopPreviewAnnotationClassId =
    ClassId.topLevel(FqName(DESKTOP_PREVIEW_ANNOTATION_FQN))

/**
 * Utils based on functions from AOSP, taken from
 * tools/adt/idea/compose-designer/src/com/android/tools/idea/compose/preview/util/PreviewElement.kt
 */

/**
 * Returns whether a `@Composable` [DESKTOP_PREVIEW_ANNOTATION_FQN] is defined in a valid location,
 * which can be either:
 * 1. Top-level functions
 * 2. Non-nested functions defined in top-level classes that have a default (no parameter)
 *    constructor
 */
private fun KtNamedFunction.isValidPreviewLocation(): Boolean {
    if (valueParameters.isNotEmpty()) return false
    if (receiverTypeReference != null) return false

    if (isTopLevel) return true

    if (parentOfType<KtNamedFunction>() == null) {
        // This is not a nested method
        val containingClass = containingClass()
        if (containingClass != null) {
            // We allow functions that are not top level defined in top level classes that have a
            // default (no parameter) constructor.
            if (containingClass.isTopLevel() && containingClass.hasDefaultConstructor()) {
                return true
            }
        }
    }
    return false
}

/**
 * Computes the qualified name of the class containing this [KtNamedFunction].
 *
 * For functions defined within a Kotlin class, returns the qualified name of that class. For
 * top-level functions, returns the JVM name of the Java facade class generated instead.
 */
internal fun KtNamedFunction.getClassName(): String? =
    if (isTopLevel) ((parent as? KtFile)?.findFacadeClass())?.qualifiedName
    else parentOfType<KtClass>()?.getQualifiedName()

/**
 * Computes the qualified name for a Kotlin Class. Returns null if the class is a kotlin built-in.
 */
private fun KtClass.getQualifiedName(): String? =
    analyze(this) {
        val classSymbol = symbol
        return when {
            classSymbol !is KaClassLikeSymbol -> null
            classSymbol.classId.isKotlinPackage() -> null
            else -> classSymbol.classId?.asFqNameString()
        }
    }

private fun ClassId?.isKotlinPackage() =
    this != null && startsWith(org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_NAME)

private fun KtClass.hasDefaultConstructor() =
    allConstructors.isEmpty().or(allConstructors.any { it.valueParameters.isEmpty() })

internal fun KtNamedFunction.composePreviewFunctionFqn() = "${getClassName()}.${name}"

@RequiresReadLock
internal fun KtNamedFunction.isValidComposablePreviewFunction(): Boolean {
    fun isValidComposablePreviewImpl(): Boolean =
        analyze(this) {
            if (!isValidPreviewLocation()) return false

            val mySymbol = symbol
            val hasComposableAnnotation = mySymbol.annotations.contains(ComposableAnnotationClassId)
            val hasPreviewAnnotation =
                mySymbol.annotations.contains(DesktopPreviewAnnotationClassId)

            return hasComposableAnnotation && hasPreviewAnnotation
        }

    return CachedValuesManager.getCachedValue(this) { cachedResult(isValidComposablePreviewImpl()) }
}

// based on AndroidComposePsiUtils.kt from AOSP
internal fun KtNamedFunction.isComposableFunction(): Boolean =
    CachedValuesManager.getCachedValue(this) {
        val hasComposableAnnotation =
            analyze(this) { symbol.annotations.contains(ComposableAnnotationClassId) }

        cachedResult(hasComposableAnnotation)
    }

private fun <T> KtNamedFunction.cachedResult(value: T) =
    CachedValueProvider.Result.create(
        // TODO: see if we can handle alias imports without ruining performance.
        value,
        this.containingKtFile,
        ProjectRootModificationTracker.getInstance(project),
    )
