/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.*
import kotlin.random.Random
import kotlin.random.nextUInt

class ColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val ktPsiFactory = KtPsiFactory(element.project)
        val uElement: UElement = element.toUElement() ?: return null
        element.text
        if (uElement is UCallExpression) {
            if (uElement.kind == UastCallKind.METHOD_CALL && uElement.methodIdentifier?.name == "Color") {
                return LineMarkerInfo(
                    element,
                    element.textRange,
                    AllIcons.General.Information,
                    null,
                    { mouseEvent, psiElement: PsiElement ->
                        psiElement.replace(
                            ktPsiFactory.createExpression(
                                "Color(0x${Random.nextUInt().toString(16)})"
                            )
                        )
                    },
                    GutterIconRenderer.Alignment.RIGHT,
                    { "change color literal" }
                )
            }
        }
        return null
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {

    }
}

//fun UElement.isIntegerLiteral(): Boolean = this is ULiteralExpression && this.value is Int
