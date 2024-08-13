/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.util.*
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Checks that every class in a [jarFile] matches one of [allowedPackagePrefixes]
 */
abstract class CheckJarPackagesTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @get:InputFile
    val jarFile: Property<RegularFile> = objects.fileProperty()

    @get:Input
    val allowedPackagePrefixes: SetProperty<String> = objects.setProperty(String::class.java)
    
    @TaskAction
    fun run() {
        ZipFile(jarFile.get().asFile).use { zip ->
            checkJarContainsExpectedPackages(zip)
        }
    }

    private fun checkJarContainsExpectedPackages(jar: ZipFile) {
        val unexpectedClasses = arrayListOf<String>()
        val allowedPrefixes = allowedPackagePrefixes.get().map { it.replace(".", "/") }

        for (entry in jar.entries()) {
            if (entry.isDirectory || !entry.name.endsWith(".class")) continue

            if (allowedPrefixes.none { prefix -> entry.name.startsWith(prefix) }) {
                unexpectedClasses.add(entry.name)
            }
        }

        if (unexpectedClasses.any()) {
            error(buildString {
                appendLine("All classes in ${jar.name} must match allowed prefixes:")
                allowedPrefixes.forEach {
                    appendLine("  * $it")
                }
                appendLine("Non-valid classes:")
                val unexpectedGroups = unexpectedClasses
                    .groupByTo(TreeMap()) { it.substringBeforeLast("/") }
                for ((_, classes) in unexpectedGroups) {
                    appendLine("  * ${classes.first()}")
                }
            })
        }
    }
}

