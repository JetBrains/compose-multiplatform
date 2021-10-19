package org.jetbrains.compose.codeeditor.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.codeeditor.CodeEditor
import org.jetbrains.compose.codeeditor.Platform
import org.jetbrains.compose.codeeditor.Project
import org.jetbrains.compose.codeeditor.createPlatformInstance
import org.jetbrains.compose.codeeditor.createProjectFile
import org.jetbrains.compose.codeeditor.diagnostics.DiagnosticElement
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.pathString
import kotlin.io.path.writeText

private val projectDirPath = Path.of(System.getProperty("java.io.tmpdir"), "tempProject")
private val filePath = Path.of(projectDirPath.pathString, "main.kt")

private fun prepareFile() {
    projectDirPath.createDirectories()
    filePath.writeText("""
        fun main() {
            val greeting = listOf("Hello", "World")
            println(greeting.joinToString(", ") + "!")
        }
        
        fun foo() {
            vl list = ArrayList<String>();
            list.add(3);
            if (true) {
                println(list[3]);
            } else {
                {
                    val str = "string"
                }
            }
        }
    """.trimIndent())
}

private fun clearTempDir() {
    filePath.deleteExisting()
    projectDirPath.deleteExisting()
}

class PlatformState {
    private val platform: Platform = createPlatformInstance()
    lateinit var project: Project

    fun init(projectDir: String) {
        platform.init()
        project = platform.openProject(projectDir)
        project.addLibraries("jars/kotlin-stdlib.jar")
    }

    fun close() {
        project.closeProject()
        platform.stop()
    }
}

fun main() = application {
    var loading by remember { mutableStateOf(true) }
    val platformState = remember { PlatformState() }

    LaunchedEffect(Unit) {
        delay(100)
        prepareFile()
        platformState.init(projectDirPath.pathString)
        loading = false
    }

    Window(
        title = if (loading) "Loading..." else "Code Editor",
        onCloseRequest = {
            platformState.close()
            clearTempDir()
            exitApplication()
        }
    ) {
        if (!loading) {
            val projectFile = remember {
                createProjectFile(
                    project = platformState.project,
                    projectDir = projectDirPath.pathString,
                    absoluteFilePath = filePath.pathString
                )
            }

            val diagnostics = remember { mutableStateListOf<DiagnosticElement>() }

            CodeEditor(
                projectFile = projectFile,
                modifier = Modifier.fillMaxSize(),
                diagnostics = diagnostics,
                onTextChange = { updateDiagnostics(it, diagnostics) }
            )
        }
    }
}
