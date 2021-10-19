package org.jetbrains.compose.codeeditor

import androidx.compose.runtime.Stable
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Stable
internal class ProjectFileImpl(
    override val project: Project,
    override val projectDir: String?,
    override val absoluteFilePath: String
) : ProjectFile {
    private val filePath: Path = Path.of(absoluteFilePath)
    override val relativeFilePath: String = projectDir?.let {
        absoluteFilePath.substring(projectDir.length + 1)
    } ?: filePath.fileName.toString()

    init {
        projectDir?.let { require(absoluteFilePath.startsWith(projectDir)) }
    }

    override fun load(): String = filePath.readText()

    override fun save(text: String) = filePath.writeText(text)

    override fun getCodeCompletionList(text: String, caretOffset: Int): List<CodeCompletionElement> {
        save(text)
        return project.getCodeCompletion(absoluteFilePath, caretOffset)
    }

    override fun getGotoDeclarationData(text: String, caretOffset: Int): GotoDeclarationData {
        save(text)
        return project.gotoDeclaration(absoluteFilePath, caretOffset)
    }
}

actual typealias GotoDeclarationTarget = org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationTarget
actual typealias GotoDeclarationData = org.jetbrains.compose.codeeditor.platform.api.GotoDeclarationData
actual typealias CodeCompletionElement = org.jetbrains.compose.codeeditor.platform.api.CodeCompletionElement
actual typealias Project = org.jetbrains.compose.codeeditor.platform.api.Project
