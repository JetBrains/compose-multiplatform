/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.intellij.CommonBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.kotlin.r4a.idea.editor.KtxEditorOptions
import java.awt.Container
import javax.swing.Action
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtxPasteFromXmlDialog(project: Project) : DialogWrapper(project, false) {
    private lateinit var panel: JPanel
    private lateinit var donTShowThisCheckBox: JCheckBox

    init {
        isModal = true
        title = "Convert Code From XML"
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun getContentPane(): Container = panel

    override fun createActions(): Array<Action> {
        setOKButtonText(CommonBundle.getYesButtonText())
        setCancelButtonText(CommonBundle.getNoButtonText())
        return arrayOf(okAction, cancelAction)
    }

    override fun doOKAction() {
        if (donTShowThisCheckBox.isSelected) {
            KtxEditorOptions.getInstance().donTShowKtxConversionDialog = true
        }
        super.doOKAction()
    }
}