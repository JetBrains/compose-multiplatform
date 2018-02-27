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

package androidx.build.checkapi

import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputFile
import java.io.File

/**
 * Task that converts the given API file to XML format.
 */
open class ApiXmlConversionTask : JavaExec() {
    @get:InputFile
    lateinit var inputApiFile: File

    @get:OutputFile
    lateinit var outputApiXmlFile: File

    init {
        maxHeapSize = "1024m"

        // Despite this tool living in ApiCheck, its purpose more fits with doclava's "purposes",
        // generation of api files in this case. Thus, I am putting this in the doclava package.
        main = "com.google.doclava.apicheck.ApiCheck"
    }

    override fun exec() {
        args = listOf("-convert2xml", inputApiFile.absolutePath, outputApiXmlFile.absolutePath)
        super.exec()
    }
}
