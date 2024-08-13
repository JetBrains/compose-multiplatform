/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class SerializeClasspathTask @Inject constructor(
    objects: ObjectFactory
) : DefaultTask() {
    @get:InputFiles
    val classpathFileCollection: ConfigurableFileCollection = objects.fileCollection()

    @get:OutputFile
    val outputFile: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun run() {
        val classpath = classpathFileCollection.files.joinToString(File.pathSeparator) { it.absolutePath }
        val outputFile = outputFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(classpath)
    }
}