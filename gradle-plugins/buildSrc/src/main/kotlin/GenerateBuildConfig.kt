import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property

/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

open class GenerateBuildConfig : DefaultTask() {
    @get:Input
    val fieldsToGenerate: MapProperty<String, Any> = project.objects.mapProperty()

    @get:Input
    val classFqName: Property<String> = project.objects.property()

    @get:OutputDirectory
    val generatedOutputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun execute() {
        val dir = generatedOutputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()

        val fqName = classFqName.get()
        val parts = fqName.split(".")
        val className = parts.last()
        val file = dir.resolve("$className.kt")
        val content = buildString {
            if (parts.size > 1) {
                appendLine("package ${parts.dropLast(1).joinToString(".")}")
            }

            appendLine()
            appendLine("/* GENERATED, DO NOT EDIT MANUALLY! */")
            appendLine("object $className {")
            for ((k, v) in fieldsToGenerate.get().entries.sortedBy { it.key }) {
                appendLine("const val $k = ${if (v is String) "\"$v\"" else v.toString()}")
            }
            appendLine("}")
        }
        file.writeText(content)
    }
}