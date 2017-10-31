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

package android.support.checkapi;

import com.google.common.io.Files;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task for updating the checked in API file with the newly generated one.
 */
public class UpdateApiTask extends DefaultTask {
    private File mNewApiFile;
    private File mNewRemovedApiFile;

    private Set<String> mWhitelistErrors = new HashSet<>();

    private File mOldApiFile;
    private File mOldRemovedApiFile;

    private File mWhitelistErrorsFile;

    @InputFile
    public File getNewApiFile() {
        return mNewApiFile;
    }

    public void setNewApiFile(File newApiFile) {
        this.mNewApiFile = newApiFile;
    }

    @InputFile
    @Optional
    public File getNewRemovedApiFile() {
        return mNewRemovedApiFile;
    }

    public void setNewRemovedApiFile(File newRemovedApiFile) {
        this.mNewRemovedApiFile = newRemovedApiFile;
    }

    @Input
    @Optional
    public Set<String> getWhitelistErrors() {
        return mWhitelistErrors;
    }

    public void setWhitelistErrors(Set<String> whitelistErrors) {
        this.mWhitelistErrors = whitelistErrors;
    }

    @OutputFile
    public File getOldApiFile() {
        return mOldApiFile;
    }

    public void setOldApiFile(File oldApiFile) {
        this.mOldApiFile = oldApiFile;
    }

    @OutputFile
    @Optional
    public File getOldRemovedApiFile() {
        return mOldRemovedApiFile;
    }

    public void setOldRemovedApiFile(File oldRemovedApiFile) {
        this.mOldRemovedApiFile = oldRemovedApiFile;
    }

    @OutputFile
    @Optional
    public File getWhitelistErrorsFile() {
        return mWhitelistErrorsFile;
    }

    public void setWhitelistErrorsFile(File whitelistErrorsFile) {
        this.mWhitelistErrorsFile = whitelistErrorsFile;
    }

    /**
     * Actually copy the file to the desired location and update the whitelist warnings file.
     */
    @TaskAction
    public void doUpdate() throws Exception {
        Files.copy(getNewApiFile(), getOldApiFile());

        if (getOldRemovedApiFile() != null) {
            if (getNewRemovedApiFile() != null) {
                Files.copy(getNewRemovedApiFile(), getOldRemovedApiFile());
            } else {
                getOldRemovedApiFile().delete();
            }
        }

        if (mWhitelistErrorsFile != null && !mWhitelistErrors.isEmpty()) {
            if (mWhitelistErrorsFile.exists()) {
                List<String> lines =
                        Files.readLines(mWhitelistErrorsFile, Charset.defaultCharset());
                mWhitelistErrors.removeAll(lines);
            }
            try (BufferedWriter writer = Files.newWriter(
                    mWhitelistErrorsFile, Charset.defaultCharset())) {
                for (String error : mWhitelistErrors) {
                    writer.write(error + "\n");
                }
            }
            getLogger().lifecycle("Whitelisted " + mWhitelistErrors.size() + " error(s)...");
        }

        getLogger().lifecycle("Wrote public API definition to " + mOldApiFile.getName());
    }
}
