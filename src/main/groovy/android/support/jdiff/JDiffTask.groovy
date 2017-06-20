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

package android.support.jdiff;

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.Optional

public class JDiffTask extends Javadoc {

    @InputFiles
    Collection<File> docletpath

    @InputFile
    File oldApiXmlFile

    @InputFile
    File newApiXmlFile

    /**
     * Relative path to the Javadoc corresponding to the old API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    @Input
    @Optional
    String oldJavadocPrefix

    /**
     * Relative path to the Javadoc corresponding to the new API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    @Input
    String newJavadocPrefix

    // HTML diff files will be placed in destinationDir, which is defined by the superclass.

    @Input
    boolean stats = true

    public JDiffTask() {
        failOnError = true
        options.doclet = "jdiff.JDiff"
        options.encoding("UTF-8")
        maxMemory = "1280m"
    }

    /**
     * Sets the doclet path which has the {@code com.gogole.doclava.Doclava} class.
     * <p>
     * This option will override any doclet path set in this instance's
     * {@link #getOptions() JavadocOptions}.
     *
     * @see MinimalJavadocOptions#setDocletpath(java.util.List)
     */
    public void setDocletpath(Collection<File> docletpath) {
        this.docletpath = docletpath

        // Go ahead and keep the docletpath in our JavadocOptions object in sync.
        options.docletpath = docletpath as List
    }

    /**
     * "Configures" this JDiffTask with parameters that might not be at their final values
     * until this task is run.
     */
    private void configureJDiffTask() {
        options.docletpath = getDocletpath() as List

        if (getStats()) {
            options.addStringOption('stats')
        }

        File oldApiXmlFile = getOldApiXmlFile()
        File newApiXmlFile = getNewApiXmlFile()

        File oldApiXmlFileDir = oldApiXmlFile.parentFile
        File newApiXmlFileDir = newApiXmlFile.parentFile

        if (oldApiXmlFileDir) {
            options.addStringOption('oldapidir', oldApiXmlFileDir.absolutePath)
        }
        // For whatever reason, jdiff appends .xml to the file name on its own.
        // Strip the .xml off the end of the file name
        options.addStringOption('oldapi',
                oldApiXmlFile.name.substring(0, oldApiXmlFile.name.length() - 4))
        if (newApiXmlFileDir) {
            options.addStringOption('newapidir', newApiXmlFileDir.absolutePath)
        }
        options.addStringOption('newapi',
                newApiXmlFile.name.substring(0, newApiXmlFile.name.length() - 4))

        String oldJavadocPrefix = getOldJavadocPrefix()
        String newJavadocPrefix = getNewJavadocPrefix()

        if (oldJavadocPrefix) {
            options.addStringOption('javadocold', oldJavadocPrefix)
        }
        if (newJavadocPrefix) {
            options.addStringOption('javadocnew', newJavadocPrefix)
        }
    }

    @Override
    public void generate() {
        configureJDiffTask();
        super.generate();
    }
}
