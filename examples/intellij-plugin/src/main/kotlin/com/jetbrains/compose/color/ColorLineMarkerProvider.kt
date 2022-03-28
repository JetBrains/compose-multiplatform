/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposePanel
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.*
import javax.swing.JComponent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.intellij.openapi.application.ApplicationManager

class ColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val project = element.project
        val ktPsiFactory = KtPsiFactory(project)
        val uElement: UElement = element.toUElement() ?: return null
        if (uElement is UCallExpression) {
            if (uElement.kind == UastCallKind.METHOD_CALL && uElement.methodIdentifier?.name == "Color") {
                return LineMarkerInfo(
                    element,
                    element.textRange,
                    AllIcons.General.Information,
                    null,
                    { mouseEvent, psiElement: PsiElement ->

                        class ChooseColorDialog() : DialogWrapper(project) {
                            val colorState = mutableStateOf(Color(0xff_ff_ff_ff))

                            init {
                                title = "Choose color"
                                init()
                            }

                            override fun createCenterPanel(): JComponent =
                                ComposePanel().apply {
                                    setBounds(0, 0, 400, 400)
                                    setContent {
                                        var color by remember { colorState }
                                        ColorPallet(colorState)
                                    }
                                }
                        }

                        val chooseColorDialog = ChooseColorDialog()
                        val result = chooseColorDialog.showAndGet()
                        if (result) {
                            val color = chooseColorDialog.colorState.value
                            ApplicationManager.getApplication().runWriteAction {
                                psiElement.replace(
                                    ktPsiFactory.createExpression(
                                        "Color(${color.toHexString()})"
                                    )
                                )
                            }
                        }
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

