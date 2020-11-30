package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.compose.desktop.application.internal.OS
import org.jetbrains.compose.desktop.application.internal.cliArg
import org.jetbrains.compose.desktop.application.internal.currentOS
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class AbstractJvmToolOperationTask(private val toolName: String) : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory
    @get:Inject
    protected abstract val providers: ProviderFactory
    @get:Inject
    protected abstract val execOperations: ExecOperations
    @get:Inject
    protected abstract val fileOperations: FileOperations

    @get:Input
    @get:Optional
    val freeArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:OutputDirectory
    val destinationDir: DirectoryProperty = objects.directoryProperty()

    @get:Internal
    val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    @get:Internal
    val verbose: Property<Boolean> = objects.notNullProperty<Boolean>().apply {
        val composeVerbose = providers
            .gradleProperty("compose.desktop.verbose")
            .map { "true".equals(it, ignoreCase = true) }
        set(providers.provider { logger.isDebugEnabled }.orElse(composeVerbose))
    }

    protected open fun makeArgs(tmpDir: File): MutableList<String> = arrayListOf<String>().apply {
        freeArgs.orNull?.forEach { add(it) }
    }
    protected open fun prepareWorkingDir(tmpDir: File) {}
    protected open fun configureExec(exec: ExecSpec) {}
    protected open fun checkResult(result: ExecResult) {
        result.assertNormalExitValue()
    }

    @TaskAction
    fun run() {
        val javaHomePath = javaHome.get()

        val jtool = File(javaHomePath).resolve("bin/${executableName(toolName)}")
        check(jtool.isFile) {
            "Invalid JDK: $jtool is not a file! \n" +
                    "Ensure JAVA_HOME or buildSettings.javaHome is set to JDK 14 or newer"
        }

        fileOperations.delete(destinationDir)
        val tmpDir = Files.createTempDirectory("compose-${toolName}").toFile().apply {
            deleteOnExit()
        }
        try {
            val args = makeArgs(tmpDir)
            prepareWorkingDir(tmpDir)
            val composeBuildDir = project.buildDir.resolve("compose").apply { mkdirs() }
            val argsFile = composeBuildDir.resolve("${name}.args.txt")
            argsFile.writeText(args.joinToString("\n"))

            execOperations.exec { exec ->
                configureExec(exec)
                exec.executable = jtool.absolutePath
                exec.setArgs(listOf("@${argsFile.absolutePath}"))
            }.also { checkResult(it) }
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}