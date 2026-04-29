import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") 
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

kotlin {
    jvm()
    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":common"))
            }
        }
    }
}

fun printCommand(label: String, vararg command: String) {
    println(">>> $label")
    try {
        val output = providers.exec {
            commandLine(*command)
            isIgnoreExitValue = true
        }
        output.standardOutput.asText.get().trimEnd().takeIf { it.isNotEmpty() }?.let(::println)
        output.standardError.asText.get().trimEnd().takeIf { it.isNotEmpty() }?.let(::println)
        println("exitCode=${output.result.get().exitValue}")
    } catch (e: Throwable) {
        println("<failed to run ${command.joinToString(" ")}: ${e.message}>")
    }
}

fun printTool(tool: String) {
    printCommand("$tool location", "bash", "-lc", "command -v $tool || true")
    printCommand("$tool version", "bash", "-lc", "$tool --version 2>&1 | sed -n '1,8p'")
}

fun dumpFile(file: File) {
    println("----- ${file.relativeToOrSelf(projectDir)} -----")
    if (file.isFile) {
        file.useLines { lines ->
            lines.take(240).forEach(::println)
        }
    } else {
        println("<missing>")
    }
}

fun printNativePackagingDiagnostics(stage: String) {
    println("===== Native packaging diagnostics ($stage) =====")
    println("projectDir=$projectDir")
    println("buildDir=${layout.buildDirectory.get().asFile}")
    println("java.home=${System.getProperty("java.home")}")
    println("PATH=${System.getenv("PATH")}")
    println("JAVA_HOME=${System.getenv("JAVA_HOME") ?: "<unset>"}")
    println("GRADLE_OPTS=${System.getenv("GRADLE_OPTS") ?: "<unset>"}")
    printCommand("uname", "uname", "-a")
    printCommand("os-release", "bash", "-lc", "sed -n '1,40p' /etc/os-release")
    printCommand("java version", "java", "-version")
    printTool("jpackage")
    printTool("fakeroot")
    printTool("dpkg")
    printTool("dpkg-deb")
    printTool("rpm")
    printTool("rpmbuild")
    printCommand("disk usage", "df", "-h", projectDir.absolutePath)

    val composeDir = layout.buildDirectory.dir("compose").get().asFile
    println(">>> build/compose tree")
    if (composeDir.exists()) {
        composeDir.walkTopDown().maxDepth(8).forEach { println(it.relativeToOrSelf(projectDir)) }
    } else {
        println("<missing>")
    }

    if (composeDir.exists()) {
        composeDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".args.txt") }
            .forEach(::dumpFile)
    }
    println("================================================")
}

val printNativePackagingDiagnostics by tasks.registering {
    doLast {
        printNativePackagingDiagnostics("after packaging task")
    }
}

tasks.matching { it.name == "packageDeb" || it.name == "packageReleaseDeb" }.configureEach {
    doFirst {
        printNativePackagingDiagnostics("before $path")
    }
    finalizedBy(printNativePackagingDiagnostics)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinMultiplatformComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
