package org.jetbrains.codeviewer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import org.jetbrains.codeviewer.platform.HomeFolder
import org.jetbrains.codeviewer.ui.common.LocalTheme
import org.jetbrains.codeviewer.ui.common.Settings
import org.jetbrains.codeviewer.ui.common.Theme
import org.jetbrains.codeviewer.ui.editor.Editors
import org.jetbrains.codeviewer.ui.filetree.FileTree

@Composable
fun MainView() {
    val codeViewer = remember {
        val editors = Editors()

        CodeViewer(
            editors = editors,
            fileTree = FileTree(HomeFolder, editors),
            settings = Settings()
        )
    }

    DisableSelection {
        val theme = if (isSystemInDarkTheme()) Theme.dark else Theme.light

        CompositionLocalProvider(
            LocalTheme provides theme,
        ) {
            MaterialTheme(
                colors = theme.materialColors
            ) {
                Surface {
                    CodeViewerView(codeViewer)
                }
            }
        }
    }
}