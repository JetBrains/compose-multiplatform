package org.jetbrains.compose.codeeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.codeeditor.diagnostics.DiagnosticElement
import org.jetbrains.compose.codeeditor.editor.Editor
import org.jetbrains.compose.codeeditor.editor.EditorState
import org.jetbrains.compose.codeeditor.search.SearchBar
import org.jetbrains.compose.codeeditor.statusbar.StatusBar
import org.jetbrains.compose.stubs.PlatformStub
import org.jetbrains.compose.stubs.isStub
import org.jetbrains.compose.codeeditor.platform.impl.IntellijPlatformWrapper

@Composable
internal actual fun CodeEditorImpl(
    projectFile: ProjectFile,
    modifier: Modifier,
    diagnostics: List<DiagnosticElement>,
    onTextChange: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val editorState = rememberSaveable {
        EditorState(
            projectFile = projectFile,
            scope = scope,
            onOuterGotoDeclaration = { _, _, _ -> }
        )
    }

    editorState.setDiagnostics(diagnostics)

    MaterialTheme(
        colors = AppTheme.colors.material,
        typography = AppTheme.typography.material
    ) {
        Surface {
            Column(modifier) {
                SearchBar(editorState.searchState)
                Editor(editorState, onTextChange, Modifier.weight(1f))
                StatusBar(editorState.diagnosticMessagesUnderCaret, editorState.busyState)
            }
        }
    }
}

actual fun createProjectFile(
    project: Project,
    projectDir: String?,
    absoluteFilePath: String
): ProjectFile = ProjectFileImpl(project, projectDir, absoluteFilePath)

actual typealias Platform = org.jetbrains.compose.codeeditor.platform.api.Platform

actual fun createPlatformInstance(): Platform = if (!isStub) IntellijPlatformWrapper() else PlatformStub()
