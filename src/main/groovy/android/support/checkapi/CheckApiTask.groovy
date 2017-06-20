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

package android.support.checkapi

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Nullable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile

import java.security.MessageDigest

/**
 * Task used to verify changes between two API files.
 * <p>
 * This task may be configured to ignore, warn, or fail with a message for a specific set of
 * Doclava-defined error codes. See {@link com.google.doclava.Errors} for a complete list of
 * supported error codes.
 * <p>
 * Specific failures may be ignored by specifying a list of SHAs in {@link #whitelistErrors}. Each
 * SHA is unique to a specific API change and is logged to the error output on failure.
 */
public class CheckApiTask extends DefaultTask {
    /** Character that resets console output color. */
    private static final String ANSI_RESET = "\u001B[0m";

    /** Character that sets console output color to red. */
    private static final String ANSI_RED = "\u001B[31m";

    /** Character that sets console output color to yellow. */
    private static final String ANSI_YELLOW = "\u001B[33m";

    /** API file that represents the existing API surface. */
    @InputFile
    File oldApiFile

    /** API file that represents the existing API surface's removals. */
    @InputFile
    File oldRemovedApiFile

    /** API file that represents the candidate API surface. */
    @InputFile
    File newApiFile

    /** API file that represents the candidate API surface's removals. */
    @InputFile
    File newRemovedApiFile

    /** Optional file containing a newline-delimited list of error SHAs to ignore. */
    @Nullable
    File whitelistErrorsFile

    @Optional
    @Nullable
    @InputFile
    File getWhiteListErrorsFileInput() {
        // Gradle requires non-null InputFiles to exist -- even with Optional -- so work around that
        // by returning null for this field if the file doesn't exist.
        if (whitelistErrorsFile && whitelistErrorsFile.exists()) {
            return whitelistErrorsFile;
        }
        return null;
    }

    /**
     * Optional list of packages to ignore.
     * <p>
     * Packages names will be matched exactly; sub-packages are not automatically recognized.
     */
    @Optional
    @Nullable
    @Input
    Collection ignoredPackages = null

    /**
     * Optional list of classes to ignore.
     * <p>
     * Class names will be matched exactly by their fully-qualified names; inner classes are not
     * automatically recognized.
     */
    @Optional
    @Nullable
    @Input
    Collection ignoredClasses = null

    /**
     * Optional set of error SHAs to ignore.
     * <p>
     * Each error SHA is unique to a specific API change.
     */
    @Optional
    @Input
    Set whitelistErrors = []

    @InputFiles
    Collection<File> doclavaClasspath

    // A dummy output file meant only to tag when this check was last ran.
    // Without any outputs, Gradle will run this task every time.
    @Optional
    @Nullable
    private File mOutputFile = null;

    @OutputFile
    public File getOutputFile() {
        return mOutputFile ?: new File(project.buildDir, "checkApi/${name}-completed")
    }

    @Optional
    public void setOutputFile(File outputFile) {
        mOutputFile = outputFile
    }

    /**
     * List of Doclava error codes to treat as errors.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    @Input
    Collection checkApiErrors

    /**
     * List of Doclava error codes to treat as warnings.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    @Input
    Collection checkApiWarnings

    /**
     * List of Doclava error codes to ignore.
     * <p>
     * See {@link com.google.doclava.Errors} for a complete list of error codes.
     */
    @Input
    Collection checkApiHidden

    /** Message to display on API check failure. */
    @Input
    String onFailMessage

    public CheckApiTask() {
        group = 'Verification'
        description = 'Invoke Doclava\'s ApiCheck tool to make sure current.txt is up to date.'
    }

    private Set<File> collectAndVerifyInputs() {
        Set<File> apiFiles = [getOldApiFile(), getNewApiFile(), getOldRemovedApiFile(),
                              getNewRemovedApiFile()] as Set
        if (apiFiles.size() != 4) {
            throw new InvalidUserDataException("""Conflicting input files:
    oldApiFile: ${getOldApiFile()}
    newApiFile: ${getNewApiFile()}
    oldRemovedApiFile: ${getOldRemovedApiFile()}
    newRemovedApiFile: ${getNewRemovedApiFile()}
All of these must be distinct files.""")
        }
        return apiFiles;
    }

    public void setCheckApiErrors(Collection errors) {
        // Make it serializable.
        checkApiErrors = errors as int[]
    }

    public void setCheckApiWarnings(Collection warnings) {
        // Make it serializable.
        checkApiWarnings = warnings as int[]
    }

    public void setCheckApiHidden(Collection hidden) {
        // Make it serializable.
        checkApiHidden = hidden as int[]
    }

    @TaskAction
    public void exec() {
        final def apiFiles = collectAndVerifyInputs()

        OutputStream errStream = new ByteArrayOutputStream()

        // If either of those gets tweaked, then this should be refactored to extend JavaExec.
        project.javaexec {
            // Put Doclava on the classpath so we can get the ApiCheck class.
            classpath(getDoclavaClasspath())
            main = 'com.google.doclava.apicheck.ApiCheck'

            minHeapSize = '128m'
            maxHeapSize = '1024m'

            // [other options] old_api.txt new_api.txt old_removed_api.txt new_removed_api.txt

            // add -error LEVEL for every error level we want to fail the build on.
            getCheckApiErrors().each { args('-error', it) }
            getCheckApiWarnings().each { args('-warning', it) }
            getCheckApiHidden().each { args('-hide', it) }

            Collection ignoredPackages = getIgnoredPackages()
            if (ignoredPackages) {
                ignoredPackages.each { args('-ignorePackage', it) }
            }
            Collection ignoredClasses = getIgnoredClasses()
            if (ignoredClasses) {
                ignoredClasses.each { args('-ignoreClass', it) }
            }

            args(apiFiles.collect( { it.absolutePath } ))

            // Redirect error output so that we can whitelist specific errors.
            errorOutput = errStream

            // We will be handling failures ourselves with a custom message.
            ignoreExitValue = true
        }

        // Load the whitelist file, if present.
        if (whitelistErrorsFile && whitelistErrorsFile.exists()) {
            whitelistErrors += whitelistErrorsFile.readLines()
        }

        // Parse the error output.
        def unparsedErrors = []
        def ignoredErrors = []
        def parsedErrors = []
        errStream.toString().split("\n").each {
            if (it) {
                def matcher = it =~ ~/^(.+):(.+): (\w+) (\d+): (.+)$/
                if (!matcher) {
                    unparsedErrors += [it]
                } else if (matcher[0][3] == "error") {
                    def hash = getShortHash(matcher[0][5]);
                    def error = matcher[0][1..-1] + [hash]
                    if (hash in whitelistErrors) {
                        ignoredErrors += [error]
                    } else {
                        parsedErrors += [error]
                    }
                }
            }
        }

        unparsedErrors.each { error -> logger.error "$ANSI_RED$error$ANSI_RESET" }
        parsedErrors.each { logger.error "$ANSI_RED${it[5]}$ANSI_RESET ${it[4]}"}
        ignoredErrors.each { logger.warn "$ANSI_YELLOW${it[5]}$ANSI_RESET ${it[4]}"}

        if (unparsedErrors || parsedErrors) {
            throw new GradleException(onFailMessage)
        }

        // Just create a dummy file upon completion. Without any outputs, Gradle will run this task
        // every time.
        File outputFile = getOutputFile()
        outputFile.parentFile.mkdirs()
        outputFile.createNewFile()
    }

    def getShortHash(src) {
        return MessageDigest.getInstance("SHA-1")
                .digest(src.toString().bytes)
                .encodeHex()
                .toString()[-7..-1]
    }
}