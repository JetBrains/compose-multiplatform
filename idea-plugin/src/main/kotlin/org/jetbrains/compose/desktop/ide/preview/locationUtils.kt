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

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal const val DESKTOP_PREVIEW_ANNOTATION_FQN = "androidx.compose.desktop.ui.tooling.preview.Preview"

/**
 * Utils based on functions from AOSP, taken from
 * tools/adt/idea/compose-designer/src/com/android/tools/idea/compose/preview/util/PreviewElement.kt
 */

/**
 *  Whether this function is properly annotated with [PREVIEW_ANNOTATION_FQN] and is defined in a valid location.
 *
 *  @see [isValidPreviewLocation]
 */
internal fun KtNamedFunction.isValidComposePreview() =
    isValidPreviewLocation() && annotationEntries.any { it.fqNameMatches(DESKTOP_PREVIEW_ANNOTATION_FQN) }

/**
 * Returns whether a `@Composable` [PREVIEW_ANNOTATION_FQN] is defined in a valid location, which can be either:
 * 1. Top-level functions
 * 2. Non-nested functions defined in top-level classes that have a default (no parameter) constructor
 *
 */
internal fun KtNamedFunction.isValidPreviewLocation(): Boolean {
    if (isTopLevel) return true

    if (parentOfType<KtNamedFunction>() == null) {
        // This is not a nested method
        val containingClass = containingClass()
        if (containingClass != null) {
            // We allow functions that are not top level defined in top level classes that have a default (no parameter) constructor.
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
 * For functions defined within a Kotlin class, returns the qualified name of that class. For top-level functions, returns the JVM name of
 * the Java facade class generated instead.
 *
 */
internal fun KtNamedFunction.getClassName(): String? =
    if (isTopLevel) ((parent as? KtFile)?.findFacadeClass())?.qualifiedName else parentOfType<KtClass>()?.getQualifiedName()


/** Computes the qualified name for a Kotlin Class. Returns null if the class is a kotlin built-in. */
private fun KtClass.getQualifiedName(): String? {
    val classDescriptor = analyze(BodyResolveMode.PARTIAL).get(BindingContext.CLASS, this) ?: return null
    return if (KotlinBuiltIns.isUnderKotlinPackage(classDescriptor) || classDescriptor.kind != ClassKind.CLASS) {
        null
    } else {
        classDescriptor.fqNameSafe.asString()
    }
}

private fun KtClass.hasDefaultConstructor() =
    allConstructors.isEmpty().or(allConstructors.any { it.valueParameters.isEmpty() })

/**
 * Determines whether this [KtAnnotationEntry] has the specified qualified name.
 * Careful: this does *not* currently take into account Kotlin type aliases (https://kotlinlang.org/docs/reference/type-aliases.html).
 *   Fortunately, type aliases are extremely uncommon for simple annotation types.
 */
private fun KtAnnotationEntry.fqNameMatches(fqName: String): Boolean {
    // For inspiration, see IDELightClassGenerationSupport.KtUltraLightSupportImpl.findAnnotation in the Kotlin plugin.
    val shortName = shortName?.asString() ?: return false
    return fqName.endsWith(shortName) && fqName == getQualifiedName()
}

/**
 * Computes the qualified name of this [KtAnnotationEntry].
 * Prefer to use [fqNameMatches], which checks the short name first and thus has better performance.
 */
private fun KtAnnotationEntry.getQualifiedName(): String? =
    analyze(BodyResolveMode.PARTIAL).get(BindingContext.ANNOTATION, this)?.fqName?.asString()


