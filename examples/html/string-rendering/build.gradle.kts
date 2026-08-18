import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import java.awt.Desktop
import javax.inject.Inject

abstract class RenderHtmlWithNode : DefaultTask() {
    @get:Input
    abstract val nodeExecutable: Property<String>

    @get:InputFile
    abstract val scriptFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun render() {
        val script = scriptFile.get().asFile
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()

        output.outputStream().use { destination ->
            execOperations.exec {
                executable(nodeExecutable.get())
                args(script.absolutePath)
                workingDir(script.parentFile.parentFile)
                standardOutput = destination
            }.assertNormalExitValue()
        }
    }
}

abstract class OpenHtml : DefaultTask() {
    @get:InputFile
    abstract val htmlFile: RegularFileProperty

    @TaskAction
    fun open() {
        val outputFile = htmlFile.get().asFile
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw GradleException("Opening a browser is not supported. Open ${outputFile.absolutePath} manually.")
        }
        Desktop.getDesktop().browse(outputFile.toURI())
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()

    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.html.core)
            }
        }
    }
}

val jvmMain = kotlin.targets.getByName("jvm").compilations.getByName("main")
val nodeJsEnvSpec = extensions.getByType<NodeJsEnvSpec>()
val previewDirectory = layout.buildDirectory.dir("preview")
val previewHtmlFile = previewDirectory.map { it.file("index.html") }

tasks.register<JavaExec>("renderHtml") {
    group = "application"
    description = "Renders the shared Compose HTML demo page to standard output."
    dependsOn(jvmMain.compileTaskProvider)
    mainClass.set("com.sample.stringrendering.MainKt")
    classpath(jvmMain.output.allOutputs)
    classpath(jvmMain.runtimeDependencyFiles)
}

tasks.register<RenderHtmlWithNode>("renderHtmlJs") {
    group = "application"
    description = "Renders the shared Compose HTML demo page to build/preview/index.html with Kotlin/JS on Node.js."
    dependsOn("jsDevelopmentExecutableCompileSync", "kotlinNodeJsSetup", "kotlinNpmInstall")

    nodeExecutable.set(nodeJsEnvSpec.executable)
    scriptFile.set(
        layout.buildDirectory.file(
            "js/packages/${project.name}/kotlin/${project.name}.js"
        )
    )
    outputFile.set(previewHtmlFile)
}

val copyPreviewResources = tasks.register<Copy>("copyPreviewResources") {
    from("src/commonMain/resources")
    into(previewDirectory)
}

tasks.register<OpenHtml>("previewHtml") {
    group = "application"
    description = "Renders the HTML with Kotlin/JS on Node.js and opens it in the default browser."
    dependsOn("renderHtmlJs", copyPreviewResources)
    htmlFile.set(previewHtmlFile)
}
