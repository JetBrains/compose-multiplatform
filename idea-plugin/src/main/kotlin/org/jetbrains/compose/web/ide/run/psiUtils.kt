/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.ide.run

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.base.facet.platform.platform
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

internal fun PsiElement.getAsJsMainFunctionOrNull(): KtNamedFunction? =
    (this as? KtNamedFunction)?.takeIf { it.isValidJsMain() }

internal fun KtNamedFunction.isValidJsMain(): Boolean =
    isTopLevel && isJsPlatform() && isMainFun()

internal fun KtNamedFunction.isJsPlatform(): Boolean =
    module?.platform?.let { platform ->
        platform in JsPlatforms.allJsPlatforms
    } ?: false

internal fun KtNamedFunction.isMainFun(): Boolean {
    if (name != "main") return false

    val parameters = valueParameters.toList()
    if (parameters.size > 1) return false

    val descriptor = resolveToDescriptorIfAny(BodyResolveMode.PARTIAL_NO_ADDITIONAL)
    return descriptor is FunctionDescriptor
            && isUnit(descriptor.returnType)
            && (parameters.isEmpty() || descriptor.hasSingleArrayOfStringsParameter())
}

private fun isUnit(type: KotlinType?): Boolean =
    type != null && KotlinBuiltIns.isUnit(type)

private fun FunctionDescriptor.hasSingleArrayOfStringsParameter(): Boolean {
    val parameter = valueParameters.singleOrNull() ?: return false
    val type = parameter.type
    val typeArgument = type.arguments.singleOrNull()?.type
    return KotlinBuiltIns.isArray(type) && KotlinBuiltIns.isString(typeArgument)
}