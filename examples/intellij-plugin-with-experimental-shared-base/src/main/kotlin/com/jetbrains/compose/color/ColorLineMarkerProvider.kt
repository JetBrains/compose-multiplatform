/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose.color

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.jetbrains.compose.IntellijTheme
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.*
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JComponent

class ColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val project = element.project
        val ktPsiFactory = KtPsiFactory(project)
        val uElement: UElement = element.toUElement() ?: return null
        if (uElement is UCallExpression) {
            if (uElement.kind == UastCallKind.METHOD_CALL && uElement.methodIdentifier?.name == "Color") {
                val colorLongValue = (uElement.valueArguments.firstOrNull() as? ULiteralExpression)?.getLongValue()
                val previousColor = try {
                    Color(colorLongValue!!)
                } catch (t: Throwable) {
                    Color(0xffffffff)
                }

                val iconSize = 20
                return LineMarkerInfo(
                    element,
                    element.textRange,
                    object : Icon {
                        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                            g?.color = java.awt.Color(
                                previousColor.red,
                                previousColor.green,
                                previousColor.blue,
                                previousColor.alpha
                            )
                            g?.fillRect(0, 0, iconSize, iconSize)
                        }

                        override fun getIconWidth(): Int = iconSize
                        override fun getIconHeight(): Int = iconSize
                    },
                    null,
                    { _, psiElement: PsiElement ->


                        class ChooseColorDialog() : DialogWrapper(project) {
                            val colorState = mutableStateOf(previousColor)

                            init {
                                title = "Choose color"
                                init()
                            }

                            override fun createCenterPanel(): JComponent =
                                ComposePanel().apply {
                                    setBounds(0, 0, 400, 400)
                                    setContent {
                                        IntellijTheme(project) {
                                            ColorPicker(colorState)
                                        }
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
