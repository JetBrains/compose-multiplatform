/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

fun showTextDialog(
    title: String,
    text: String,
    project: Project? = null
    ) {
    val wrapper: DialogWrapper = object : DialogWrapper(project, false) {
        init {
            init()
        }

        override fun createCenterPanel(): JComponent {
            val textArea = JTextArea(text).apply {
                isEditable = false
                rows = 40
                columns = 70
            }
            return JPanel(BorderLayout()).apply {
                add(ScrollPaneFactory.createScrollPane(textArea))
            }
        }
    }
    wrapper.title = title
    wrapper.show()
}
