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

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile

public class ApiXmlConversionTask extends JavaExec {

    @InputFile
    File inputApiFile

    @OutputFile
    File outputApiXmlFile

    public ApiXmlConversionTask() {
        maxHeapSize = "1024m"

        // Despite this tool living in ApiCheck, its purpose more fits with doclava's "purposes",
        // generation of api files in this case. Thus, I am putting this in the doclava package.
        setMain('com.google.doclava.apicheck.ApiCheck')
    }

    /**
     * "Configures" this ApiXmlConversionTask with parameters that might not be at their final
     * values until this task is run.
     */
    private configureApiXmlConversionTask() {
        setArgs([
                '-convert2xml',
                getInputApiFile().absolutePath,
                getOutputApiXmlFile().absolutePath
        ])
    }

    @Override
    public void exec() {
        configureApiXmlConversionTask()
        super.exec()
    }
}
