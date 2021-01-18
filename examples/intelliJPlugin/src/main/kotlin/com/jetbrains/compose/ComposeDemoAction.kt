package com.jetbrains.compose

import androidx.compose.desktop.ComposePanel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import java.awt.Dimension
import javax.swing.JComponent


/**
 * @author Konstantin Bulenkov
 */
class ComposeDemoAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        DemoDialog(e.project).show()
    }

    class DemoDialog(project: Project?) : DialogWrapper(project) {
        init {
            title = "Demo"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return ComposePanel().apply {
                setContent {
                    Button(onClick = { println("Hi") }) {
                        //Text("Hello") //todo: crashes IntelliJ
                        Box(
                            modifier = Modifier
                                .background(color = Color.Red)
                                .size(20.dp, 20.dp)
                        )
                    }
                }
                size = Dimension(50, 50)
            }
        }
    }
}