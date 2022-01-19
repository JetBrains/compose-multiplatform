package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

abstract class AbstractComposeDemoDialog(project: Project?) : DialogWrapper(project) {
    init {
        init()
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 800, 600)
            setContent {
                WidgetTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        dialogContent()
                    }
                }
            }
        }
    }

    @Composable
    abstract fun dialogContent()
}
