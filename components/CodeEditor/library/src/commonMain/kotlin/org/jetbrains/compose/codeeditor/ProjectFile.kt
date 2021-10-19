package org.jetbrains.compose.codeeditor

interface ProjectFile {
    val project: Project
    val projectDir: String?
    val absoluteFilePath: String
    val relativeFilePath: String

    fun load(): String
    fun save(text: String)

    fun getCodeCompletionList(text: String, caretOffset: Int): List<CodeCompletionElement>

    fun getGotoDeclarationData(text: String, caretOffset: Int): GotoDeclarationData
}

expect interface Project {
    fun addLibraries(paths: List<String?>?)
    fun synchronizeProjectDirectory()
    fun getCodeCompletion(path: String?, caretOffset: Int): List<CodeCompletionElement?>?
    fun gotoDeclaration(path: String?, caretOffset: Int): GotoDeclarationData?
    fun closeProject()
}

expect interface CodeCompletionElement {
    fun getName(): String?
    fun getType(): String?
    fun getTail(): String?
}

expect interface GotoDeclarationData {
    fun isIndexNotReady(): Boolean
    fun canNavigate(): Boolean
    fun isInitialElementOffsetSet(): Boolean
    fun getInitialElementStartOffset(): Int
    fun getInitialElementEndOffset(): Int
    fun getTargets(): Collection<GotoDeclarationTarget?>?
}

expect interface GotoDeclarationTarget {
    fun getPath(): String?
    fun getOffset(): Int
}


