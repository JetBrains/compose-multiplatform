package org.jetbrains.compose.codeeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.codeeditor.diagnostics.DiagnosticElement

/**
 * This composable provides basic code editing functionality with support for autosuggestion of code,
 * search and go to declaration.
 *
 * @param projectFile The [ProjectFile] that contains information about the project and the file being edited.
 * @param modifier optional [Modifier].
 * @param diagnostics List of diagnostic messages to be rendered in the code.
 * @param onTextChange Called when code text is changed.
 */
@Composable
fun CodeEditor(
    projectFile: ProjectFile,
    modifier: Modifier = Modifier,
    diagnostics: List<DiagnosticElement> = emptyList(),
    onTextChange: (String) -> Unit = {}
) = CodeEditorImpl(projectFile, modifier, diagnostics, onTextChange)

/**
 * Returns an instance of the [ProjectFile].
 *
 * @param project The project to which the file belongs.
 * @param projectDir Path to the project folder.
 * @param absoluteFilePath Absolute path to the file.
 */
expect fun createProjectFile(
    project: Project,
    projectDir: String? = null,
    absoluteFilePath: String
): ProjectFile

/**
 * Returns an instance of the [Platform].
 */
expect fun createPlatformInstance(): Platform

internal expect fun CodeEditorImpl(
    projectFile: ProjectFile,
    modifier: Modifier,
    diagnostics: List<DiagnosticElement>,
    onTextChange: (String) -> Unit
)

expect interface Platform {
    fun init()
    fun stop()
    fun openProject(rootFolder: String?): Project?
    fun openFile(filePath: String?): Project?
}
