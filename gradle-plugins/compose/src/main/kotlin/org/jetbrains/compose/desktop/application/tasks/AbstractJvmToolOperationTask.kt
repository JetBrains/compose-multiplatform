package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.work.InputChanges
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import java.io.File
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

    @get:LocalState
    protected val workingDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/$name")

    @get:OutputDirectory
    val destinationDir: DirectoryProperty = objects.directoryProperty()

    @get:Input
    @get:Optional
    val freeArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Internal
    val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    @get:Internal
    val verbose: Property<Boolean> = objects.notNullProperty<Boolean>().apply {
        set(providers.provider { logger.isDebugEnabled }.orElse(ComposeProperties.isVerbose(providers)))
    }

    protected open fun prepareWorkingDir(inputChanges: InputChanges) {
        fileOperations.delete(workingDir)
        fileOperations.mkdir(workingDir)
    }

    protected open fun makeArgs(tmpDir: File): MutableList<String> = arrayListOf<String>().apply {
        freeArgs.orNull?.forEach { add(it) }
    }

    protected open fun configureExec(exec: ExecSpec) {}
    protected open fun checkResult(result: ExecResult) {
        result.assertNormalExitValue()
    }

    @TaskAction
    fun run(inputChanges: InputChanges) {
        initState()
        val javaHomePath = javaHome.get()

        val jtool = File(javaHomePath).resolve("bin/${executableName(toolName)}")
        check(jtool.isFile) {
            "Invalid JDK: $jtool is not a file! \n" +
                    "Ensure JAVA_HOME or buildSettings.javaHome is set to JDK 14 or newer"
        }

        fileOperations.delete(destinationDir)
        prepareWorkingDir(inputChanges)
        val argsFile = workingDir.ioFile.let { dir ->
            val args = makeArgs(dir)
            dir.resolveSibling("${name}.args.txt").apply {
                writeText(args.joinToString("\n"))
            }
        }

        try {
            execOperations.exec { exec ->
                configureExec(exec)
                exec.executable = jtool.absolutePath
                exec.setArgs(listOf("@${argsFile.absolutePath}"))
            }.also { checkResult(it) }
        } finally {
            if (!ComposeProperties.preserveWorkingDir(providers).get()) {
                fileOperations.delete(workingDir)
            }
        }
        saveStateAfterFinish()
    }

    protected open fun initState() {}
    protected open fun saveStateAfterFinish() {}
}