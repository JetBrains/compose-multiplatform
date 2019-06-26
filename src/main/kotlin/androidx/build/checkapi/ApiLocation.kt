/*
 * Copyright 2018 The Android Open Source Project
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

import java.io.File

import androidx.build.Version
import java.io.Serializable

// An ApiLocation contains the filepath of a public API and restricted API of a library
data class ApiLocation(
    // file specifying the directory of API files for the library
    val apiFileDirectory: File,
    // file specifying the public API of the library
    val publicApiFile: File,
    // file specifying the restricted API (marked by the RestrictTo annotation) of the library
    val restrictedApiFile: File,
    // file specifying the API of the resources
    val resourceFile: File
) : Serializable {

    fun files() = listOf(publicApiFile, restrictedApiFile)

    fun version(): Version? {
        val text = publicApiFile.name.removeSuffix(".txt")
        if (text == "current") {
            return null
        }
        return Version(text)
    }

    companion object {
        fun fromPublicApiFile(f: File): ApiLocation {
            return ApiLocation(
                f.parentFile,
                f,
                File(f.parentFile, "restricted_" + f.name),
                File(f.parentFile, "res-" + f.name)
            )
        }
    }
}

// An ApiViolationBaselines contains the paths of the API baselines files for an API
data class ApiViolationBaselines(
    val publicApiFile: File,
    val restrictedApiFile: File,
    val apiLintFile: File
) : Serializable {

    fun files() = listOf(publicApiFile, restrictedApiFile)

    companion object {
        fun fromApiLocation(apiLocation: ApiLocation): ApiViolationBaselines {
            val publicBaselineFile =
                File(apiLocation.publicApiFile.toString().removeSuffix(".txt") + ".ignore")
            val restrictedBaselineFile =
                File(apiLocation.restrictedApiFile.toString().removeSuffix(".txt") + ".ignore")
            val apiLintBaselineFile =
                File(apiLocation.apiFileDirectory, "api_lint.ignore")
            return ApiViolationBaselines(
                publicBaselineFile,
                restrictedBaselineFile,
                apiLintBaselineFile
            )
        }
    }
}
