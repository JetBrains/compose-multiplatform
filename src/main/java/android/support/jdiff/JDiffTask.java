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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.external.javadoc.MinimalJavadocOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * JDiff task to compare API changes.
 */
public class JDiffTask extends Javadoc {

    private Collection<File> mDocletpath;

    private File mOldApiXmlFile;

    private File mNewApiXmlFile;

    /**
     * Relative path to the Javadoc corresponding to the old API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    private String mOldJavadocPrefix;

    /**
     * Relative path to the Javadoc corresponding to the new API, relative to
     * "${destinationDir}/changes". Should end with the directory separator (usually '/').
     */
    private String mNewJavadocPrefix;

    // HTML diff files will be placed in destinationDir, which is defined by the superclass.

    private boolean mStats = true;

    public JDiffTask() {
        setFailOnError(true);
        getOptions().setDoclet("jdiff.JDiff");
        getOptions().setEncoding("UTF-8");
        setMaxMemory("1280m");
    }

    public void setOldApiXmlFile(File file) {
        mOldApiXmlFile = file;
    }

    @InputFile
    public File getOldApiXmlFile() {
        return mOldApiXmlFile;
    }

    public void setNewApiXmlFile(File file) {
        mNewApiXmlFile = file;
    }

    @InputFile
    public File getNewApiXmlFile() {
        return mNewApiXmlFile;
    }

    @Optional
    public void setOldJavadocPrefix(String prefix) {
        mOldJavadocPrefix = prefix;
    }

    @Optional
    @Input
    public String getOldJavadocPrefix() {
        return mOldJavadocPrefix;
    }

    public void setNewJavadocPrefix(String prefix) {
        mNewJavadocPrefix = prefix;
    }

    @Input
    public String getNewJavadocPrefix() {
        return mNewJavadocPrefix;
    }

    public void setStats(boolean enabled) {
        mStats = enabled;
    }

    @Input
    public boolean getStats() {
        return mStats;
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
        mDocletpath = docletpath;

        // Go ahead and keep the mDocletpath in our JavadocOptions object in sync.
        getOptions().setDocletpath(new ArrayList<>(docletpath));
    }

    @InputFiles
    public Collection<File> getDocletpath() {
        return mDocletpath;
    }

    /**
     * "Configures" this JDiffTask with parameters that might not be at their final values
     * until this task is run.
     */
    private void configureJDiffTask() {
        CoreJavadocOptions options = (CoreJavadocOptions) getOptions();

        options.setDocletpath(new ArrayList<>(mDocletpath));

        if (getStats()) {
            options.addStringOption("stats");
        }

        File oldApiXmlFile = getOldApiXmlFile();
        File newApiXmlFile = getNewApiXmlFile();

        File oldApiXmlFileDir = oldApiXmlFile.getParentFile();
        File newApiXmlFileDir = newApiXmlFile.getParentFile();

        if (oldApiXmlFileDir.exists()) {
            options.addStringOption("oldapidir", oldApiXmlFileDir.getAbsolutePath());
        }
        // For whatever reason, jdiff appends .xml to the file name on its own.
        // Strip the .xml off the end of the file name
        options.addStringOption("oldapi",
                oldApiXmlFile.getName().substring(0, oldApiXmlFile.getName().length() - 4));
        if (newApiXmlFileDir.exists()) {
            options.addStringOption("newapidir", newApiXmlFileDir.getAbsolutePath());
        }
        options.addStringOption("newapi",
                newApiXmlFile.getName().substring(0, newApiXmlFile.getName().length() - 4));

        String oldJavadocPrefix = getOldJavadocPrefix();
        String newJavadocPrefix = getNewJavadocPrefix();

        if (oldJavadocPrefix != null) {
            options.addStringOption("javadocold", oldJavadocPrefix);
        }
        if (newJavadocPrefix != null) {
            options.addStringOption("javadocnew", newJavadocPrefix);
        }
    }

    @Override
    public void generate() {
        configureJDiffTask();
        super.generate();
    }
}
