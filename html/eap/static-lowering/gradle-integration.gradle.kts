/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Application build.gradle.kts example; keep the tool outside source directories.
val lowerer = layout.projectDirectory.dir("../compose/html/eap/static-lowering")
val authoredHtml = layout.projectDirectory.dir("src/commonMain/kotlin")
val generatedHtml = layout.buildDirectory.dir("generated/static-html")

val buildHtmlLowerer by tasks.registering(Exec::class) {
    inputs.dir(lowerer.dir("src"))
    inputs.file(lowerer.file("build.gradle.kts"))
    inputs.file(lowerer.file("settings.gradle.kts"))
    outputs.dir(lowerer.dir("build/install/ssr-static-lowering"))
    commandLine(lowerer.file("gradlew").asFile.absolutePath, "-p", lowerer.asFile.absolutePath, "installDist")
    if (gradle.startParameter.isOffline) args("--offline")
}
val lowerHtml by tasks.registering(Exec::class) {
    dependsOn(buildHtmlLowerer)
    inputs.dir(authoredHtml)
    inputs.dir(lowerer.dir("build/install/ssr-static-lowering"))
    outputs.dir(generatedHtml)
    // Remove stale generated files.
    doFirst { delete(generatedHtml) }
    commandLine(
        lowerer.file("build/install/ssr-static-lowering/bin/ssr-static-lowering").asFile.absolutePath,
        authoredHtml.asFile.absolutePath,
        generatedHtml.get().asFile.absolutePath,
    )
}
kotlin.compilerOptions {
    optIn.add("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
}
kotlin.sourceSets.named("commonMain") {
    kotlin.setSrcDirs(listOf(generatedHtml))
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(lowerHtml)
}
