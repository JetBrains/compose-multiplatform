package org.jetbrains.codeviewer.ui

import org.jetbrains.codeviewer.ui.common.Settings
import org.jetbrains.codeviewer.ui.editor.Editors
import org.jetbrains.codeviewer.ui.filetree.FileTree

class CodeViewer(
    val editors: Editors,
    val fileTree: FileTree,
    val settings: Settings
)