package org.jetbrains.compose.intellij.platform.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

class ComposeDemoAction2 : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ComposeDemoDialog2(e.project).show()
    }
}

class ComposeDemoDialog2(project: Project?) : AbstractComposeDemoDialog(project) {
    init {
        title = "Demo Dialog 2"
    }

    @Composable
    override fun dialogContent() {
        Row {
            Box(
                modifier = Modifier.fillMaxHeight().weight(1f)
            ) {
                LazyScrollable()
            }
        }
    }
}
