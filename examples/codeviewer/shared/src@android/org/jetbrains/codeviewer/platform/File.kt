package org.jetbrains.codeviewer.platform

lateinit var _HomeFolder: java.io.File
actual val HomeFolder: File get() = _HomeFolder.toProjectFile()