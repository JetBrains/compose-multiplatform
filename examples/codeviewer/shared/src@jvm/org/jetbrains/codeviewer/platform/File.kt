@file:Suppress("NewApi")

package org.jetbrains.codeviewer.platform

actual val HomeFolder: File get() = java.io.File(System.getProperty("user.home")).toProjectFile()