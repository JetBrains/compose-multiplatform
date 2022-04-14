/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package com.jetbrains.compose.benchmark

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PerformanceInfoDialog : DialogWrapper(true) {
    private val jbTextArea = JBTextArea()
    private var pause: Boolean = false

    init {
        title = "PerformanceInfoDialog"
        size.setSize(800.0, 800.0)
        init()
    }

    public fun setText(text: String) {
        jbTextArea.text = text
    }

    public fun isPaused() = pause

    override fun createCenterPanel(): JComponent = panel {
        row {
            cell(jbTextArea)
        }
        row {
            button("Pause") {
                pause = !pause
            }
        }
    }
}
