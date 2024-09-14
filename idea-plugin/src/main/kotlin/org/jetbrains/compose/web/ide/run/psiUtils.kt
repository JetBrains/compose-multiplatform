/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.ide.run

import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.base.facet.platform.platform
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.base.psi.KotlinPsiHeuristics
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.Variance

internal fun PsiElement.getAsJsMainFunctionOrNull(): KtNamedFunction? =
    (this as? KtNamedFunction)?.takeIf { it.isValidJsMain() }

internal fun KtNamedFunction.isValidJsMain(): Boolean = isTopLevel && isJsPlatform() && isMainFun()

internal fun KtNamedFunction.isJsPlatform(): Boolean =
    module?.platform?.let { platform -> platform in JsPlatforms.allJsPlatforms } == true

internal fun KtNamedFunction.isMainFun(): Boolean =
    isMainFromPsiOnly(this) && isMainFromAnalysis(this)

//////////////////////////////////////////////////////////////////
// Copied and simplified from PsiOnlyKotlinMainFunctionDetector //
//////////////////////////////////////////////////////////////////
@RequiresReadLock
private fun isMainFromPsiOnly(function: KtNamedFunction): Boolean {
    if (function.isLocal || function.typeParameters.isNotEmpty()) {
        return false
    }

    val isTopLevel = function.isTopLevel
    val parameterCount =
        function.valueParameters.size + (if (function.receiverTypeReference != null) 1 else 0)

    if (parameterCount == 0) {
        if (!isTopLevel) {
            return false
        }

        if (
            !function.languageVersionSettings.supportsFeature(
                LanguageFeature.ExtendedMainConvention
            )
        ) {
            return false
        }
    } else if (parameterCount == 1 && !isMainCheckParameter(function)) {
        return false
    } else {
        return false
    }

    if ((KotlinPsiHeuristics.findJvmName(function) ?: function.name) != "main") {
        return false
    }

    if (!isTopLevel && !KotlinPsiHeuristics.hasJvmStaticAnnotation(function)) {
        return false
    }

    val returnTypeReference = function.typeReference
    return !(returnTypeReference != null &&
        !KotlinPsiHeuristics.typeMatches(returnTypeReference, StandardClassIds.Unit))
}

private fun isMainCheckParameter(function: KtNamedFunction): Boolean {
    val receiverTypeReference = function.receiverTypeReference
    if (receiverTypeReference != null) {
        return KotlinPsiHeuristics.typeMatches(
            receiverTypeReference,
            StandardClassIds.Array,
            StandardClassIds.String,
        )
    }

    val parameter = function.valueParameters.singleOrNull() ?: return false
    val parameterTypeReference = parameter.typeReference ?: return false

    return when {
        parameter.isVarArg ->
            KotlinPsiHeuristics.typeMatches(parameterTypeReference, StandardClassIds.String)
        else ->
            KotlinPsiHeuristics.typeMatches(
                parameterTypeReference,
                StandardClassIds.Array,
                StandardClassIds.String,
            )
    }
}

//////////////////////////////////////////////////////////////////////
// Copied and simplified from SymbolBasedKotlinMainFunctionDetector //
//////////////////////////////////////////////////////////////////////
private fun isMainFromAnalysis(function: KtNamedFunction): Boolean {
    if (function.isLocal || function.typeParameters.isNotEmpty()) {
        return false
    }

    val supportsExtendedMainConvention =
        function.languageVersionSettings.supportsFeature(LanguageFeature.ExtendedMainConvention)

    val isTopLevel = function.isTopLevel
    val parameterCount =
        function.valueParameters.size + (if (function.receiverTypeReference != null) 1 else 0)

    if (parameterCount == 0) {
        if (!isTopLevel || !supportsExtendedMainConvention) {
            return false
        }
    } else if (parameterCount > 1) {
        return false
    }

    analyze(function) {
        if (parameterCount == 1) {
            val parameterTypeReference =
                function.receiverTypeReference
                    ?: function.valueParameters[0].typeReference
                    ?: return false

            val parameterType = parameterTypeReference.type
            if (
                !parameterType.isResolvedClassType() ||
                    !parameterType.isSubtypeOf(buildMainParameterType())
            ) {
                return false
            }
        }

        val functionSymbol = function.symbol
        if (functionSymbol !is KaNamedFunctionSymbol) {
            return false
        }

        if (functionSymbol.name.identifier != "main") {
            return false
        }

        if (!function.returnType.isUnitType) {
            return false
        }
    }
    return true
}

private fun KaSession.buildMainParameterType(): KaType =
    buildClassType(StandardClassIds.Array) {
        val argumentType =
            buildClassType(StandardClassIds.String) { nullability = KaTypeNullability.NON_NULLABLE }

        argument(argumentType, Variance.OUT_VARIANCE)
        nullability = KaTypeNullability.NULLABLE
    }

private fun KaType.isResolvedClassType(): Boolean =
    when (this) {
        is KaClassType -> typeArguments.mapNotNull { it.type }.all { it.isResolvedClassType() }
        else -> false
    }
