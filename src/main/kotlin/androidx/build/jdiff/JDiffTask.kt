/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build.jdiff

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.MinimalJavadocOptions

import java.io.File
import java.util.ArrayList

/**
 * JDiff task to compare API changes.
 */
open class JDiffTask : Javadoc() {

    /**
     * Sets the doclet path, which will be used to locate the `com.google.doclava.Doclava` class.
     *
     *
     * This option will override any doclet path set in this instance's
     * [JavadocOptions][.getOptions].
     *
     * @see MinimalJavadocOptions.setDocletpath
     */
    @get:InputFiles
    var docletpath: Collection<File>? = null
        set(docletpath) {
            field = docletpath
            // Go ahead and keep the mDocletpath in our JavadocOptions object in sync.
            options.docletpath = ArrayList(docletpath)
        }

    @get:InputFile
    lateinit var oldApiXmlFile: File

    @get:InputFile
    lateinit var newApiXmlFile: File

    /**
     * Relative path to the Javadoc corresponding to the old API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    @get:[Input Optional]
    var oldJavadocPrefix: String? = null

    /**
     * Relative path to the Javadoc corresponding to the new API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    @get:[Input Optional]
    var newJavadocPrefix: String? = null

    // HTML diff files will be placed in destinationDir, which is defined by the superclass.

    @get:Input
    var stats = true

    init {
        isFailOnError = true
        options.doclet = "jdiff.JDiff"
        options.encoding = "UTF-8"
        maxMemory = "1280m"
    }

    /**
     * Configures this JDiffTask with parameters that might not be at their final values
     * until this task is run.
     */
    private fun configureJDiffTask() {
        val options = options as CoreJavadocOptions

        options.docletpath = ArrayList(docletpath!!)

        if (stats) {
            options.addStringOption("stats")
        }

        val oldApiXmlFileDir = oldApiXmlFile.parentFile
        val newApiXmlFileDir = newApiXmlFile.parentFile

        if (oldApiXmlFileDir.exists()) {
            options.addStringOption("oldapidir", oldApiXmlFileDir.absolutePath)
        }
        // For whatever reason, jdiff appends .xml to the file name on its own.
        // Strip the .xml off the end of the file name
        options.addStringOption("oldapi",
                oldApiXmlFile.name.substring(0, oldApiXmlFile.name.length - 4))
        if (newApiXmlFileDir.exists()) {
            options.addStringOption("newapidir", newApiXmlFileDir.absolutePath)
        }
        options.addStringOption("newapi",
                newApiXmlFile.name.substring(0, newApiXmlFile.name.length - 4))

        if (oldJavadocPrefix != null) {
            options.addStringOption("javadocold", oldJavadocPrefix)
        }
        if (newJavadocPrefix != null) {
            options.addStringOption("javadocnew", newJavadocPrefix)
        }
    }

    public override fun generate() {
        configureJDiffTask()
        super.generate()
    }
}
