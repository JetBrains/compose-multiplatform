/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.doclava

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import java.io.File

// external/doclava/src/com/google/doclava/Errors.java
private val DEFAULT_DOCLAVA_ERRORS = setOf<Int>(
        101,  // unresolved link
        103,  // unknown tag
        104   // unknown param name
)

private val DEFAULT_DOCLAVA_WARNINGS = setOf<Int>(121 /* hidden type param */)

private val DEFAULT_DOCLAVA_HIDDEN = setOf<Int>(
        111,  // hidden super class
        113   // @deprecation mismatch
)

private fun <E> CoreJavadocOptions.addMultilineMultiValueOption(
        name: String, values: Collection<E>) {
    addMultilineMultiValueOption(name).setValue(values.map { listOf(it.toString()) })
}

open class DoclavaTask : Javadoc() {

    // All lowercase name to match MinimalJavadocOptions#docletpath
    private var docletpath: List<File> = emptyList()

    // doclava error types which will cause the build to fail
    @Input
    var doclavaErrors = DEFAULT_DOCLAVA_ERRORS

    @Input
    var doclavaWarnings = DEFAULT_DOCLAVA_WARNINGS

    // spammy doclava warnings which we want to hide
    @Input
    var doclavaHidden = DEFAULT_DOCLAVA_HIDDEN

    /**
     * If non-null, the list of packages that will be treated as if they were
     * marked with {@literal @hide}.<br>
     * Packages names will be matched exactly; sub-packages are not automatically recognized.
     */
    @Optional
    @Input
    var hiddenPackages: Collection<String>? = null

    /**
     * If non-null and not-empty, the whitelist of packages that will be present in the generated
     * stubs; if null or empty, then all packages have stubs generated.<br>
     * Wildcards are accepted.
     */
    @Optional
    @Input
    var stubPackages: Set<String>? = null

    @Input
    var generateDocs = true

    /**
     * If non-null, the location of where to place the generated api file.
     * If this is non-null, then {@link #removedApiFile} must be non-null as well.
     */
    @Optional
    @OutputFile
    var apiFile: File? = null

    /**
     * If non-null, the location of where to place the generated removed api file.
     * If this is non-null, then {@link #apiFile} must be non-null as well.
     */
    @Optional
    @OutputFile
    var removedApiFile: File? = null

    /**
     * If non-null, the location of the generated keep list.
     */
    @Optional
    @OutputFile
    var keepListFile: File? = null

    /**
     * If non-null, the location to put the generated stub sources.
     */
    @Optional
    @OutputDirectory
    var stubsDir: File? = null

    init {
        setFailOnError(true)
        options.doclet = "com.google.doclava.Doclava"
        options.encoding("UTF-8")
        options.quiet()
        // doclava doesn't understand '-doctitle'
        title = null
        maxMemory = "1280m"
        // If none of generateDocs, apiFile, keepListFile, or stubJarsDir are true, then there is
        // no work to do.
        onlyIf({ generateDocs || apiFile != null || keepListFile != null || stubsDir != null })
    }

    /**
     * The doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#getDocletpath()
     */
    @InputFiles
    fun getDocletpath(): List<File> {
        return docletpath
    }

    /**
     * Sets the doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * This option will override any doclet path set in this instance's {@link #options JavadocOptions}.
     * @see MinimalJavadocOptions#setDocletpath(java.util.List)
     */
    fun setDocletpath(docletpath: Collection<File>) {
        this.docletpath = docletpath.toList()
        // Go ahead and keep the docletpath in our JavadocOptions object in sync.
        options.docletpath = docletpath.toList()
    }

    fun setDoclavaErrors(errors: List<Int> ) {
        // Make it serializable.
        doclavaErrors = errors.toSet()
    }

    fun setDoclavaWarnings(warnings: List<Int>) {
        // Make it serializable.
        doclavaWarnings = warnings.toSet()
    }

    fun setDoclavaHidden(hidden: List<Int>) {
        // Make it serializable.
        doclavaHidden = hidden.toSet()
    }

    /**
     * "Configures" this DoclavaTask with parameters that might not be at their final values
     * until this task is run.
     */
    private fun configureDoclava() = (options as CoreJavadocOptions).apply {

        docletpath = this@DoclavaTask.docletpath

        // configure doclava error/warning/hide levels
        addMultilineMultiValueOption("hide", doclavaHidden)
        addMultilineMultiValueOption("warning", doclavaWarnings)
        addMultilineMultiValueOption("error", doclavaErrors)

        if (hiddenPackages != null) {
            addMultilineMultiValueOption("hidePackage", hiddenPackages!!)
        }

        if (!generateDocs) {
            addOption(DoclavaJavadocOptionFileOption("nodocs"))
        }

        // If requested, generate the API files.
        if (apiFile != null) {
            addFileOption("api", apiFile)
            addFileOption("removedApi", removedApiFile)
        }

        // If requested, generate the keep list.
        addFileOption("proguard", keepListFile)

        // If requested, generate stubs.
        if (stubsDir != null) {
            addFileOption("stubs", stubsDir)
            val stubs = stubPackages
            if (stubs != null) {
                addStringOption("stubpackages", stubs.joinToString(":"))
            }
        }
        // Always treat this as an Android docs task.
        addOption(DoclavaJavadocOptionFileOption("android"))
    }

    override fun generate() {
        configureDoclava()
        super.generate()
    }
}

