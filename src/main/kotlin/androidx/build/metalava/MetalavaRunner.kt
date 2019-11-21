/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.checkapi.ApiLocation
import androidx.build.java.JavaCompileInputs
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import java.io.File

// MetalavaRunner stores common configuration for executing Metalava

fun Project.getMetalavaConfiguration(): Configuration {
    return configurations.findByName("metalava") ?: configurations.create("metalava") {
        val dependency = dependencies.create("com.android:metalava:1.3.0:shadow@jar")
        it.dependencies.add(dependency)
    }
}

fun Project.runMetalavaWithArgs(configuration: Configuration, args: List<String>) {
    javaexec {
        it.classpath = checkNotNull(configuration) { "Configuration not set." }
        it.main = "com.android.tools.metalava.Driver"
        it.args = listOf(
            "--no-banner",
            "--hide",
            "HiddenSuperclass", // We allow having a hidden parent class
            "--error",
            "ReferencesDeprecated"
        ) + args
    }
}

// Metalava arguments to hide all experimental API surfaces.
val HIDE_EXPERIMENTAL_ARGS: List<String> = listOf(
    "--hide-annotation", "androidx.annotation.experimental.Experimental",
    "--hide-annotation", "kotlin.Experimental",
    "--hide-meta-annotation", "androidx.annotation.experimental.Experimental",
    "--hide-meta-annotation", "kotlin.Experimental"
)

val API_LINT_ARGS: List<String> = listOf(
    "--api-lint",
    "--hide",
    listOf(
        // The list of checks that are hidden as they are not useful in androidx
        "Enum", // Enums are allowed to be use in androidx
        "CallbackInterface", // With target Java 8, we have default methods
        "ProtectedMember", // We allow using protected members in androidx
        "ManagerLookup", // Managers in androidx are not the same as platfrom services
        "ManagerConstructor",
        "RethrowRemoteException", // This check is for calls into system_server
        "PackageLayering", // This check is not relevant to androidx.* code.
        "UserHandle", // This check is not relevant to androidx.* code.

        // List of checks that have bugs, but should be enabled once fixed.
        "GetterSetterNames", // b/135498039
        "StaticUtils", // b/135489083
        "AllUpper", // b/135708486
        "StartWithLower", // b/135710527

        // The list of checks that are API lint warnings and are yet to be enabled
        "TopLevelBuilder",
        "BuilderSetStyle",
        "ExecutorRegistration",
        "StreamFiles",
        "ParcelableList",
        "NotCloseable",
        "UserHandleName",
        "UseIcu",
        "NoByteOrShort",
        "CommonArgsFirst",
        "SamShouldBeLast",
        "MissingJvmStatic",

        // We should only treat these as warnings
        "IntentBuilderName",
        "OnNameExpected"
    ).joinToString(),
    "--error",
    listOf(
        "MinMaxConstant",
        "MissingBuild",
        "SetterReturnsThis",
        "OverlappingConstants",
        "IllegalStateException",
        "ListenerLast",
        "AbstractInner",
        "ArrayReturn",
        "MethodNameTense"
    ).joinToString()
)

sealed class GenerateApiMode {
    object PublicApi : GenerateApiMode()
    object RestrictedApi : GenerateApiMode()
    object ExperimentalApi : GenerateApiMode()
}

sealed class ApiLintMode {
    class CheckBaseline(val apiLintBaseline: File) : ApiLintMode()
    object Skip : ApiLintMode()
}

// Generates all of the specified api files
fun Project.generateApi(
    files: JavaCompileInputs,
    apiLocation: ApiLocation,
    tempDir: File,
    apiLintMode: ApiLintMode,
    includeRestrictedApis: Boolean
) {
    generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation.publicApiFile, tempDir, GenerateApiMode.PublicApi, apiLintMode)
    generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation.experimentalApiFile, tempDir, GenerateApiMode.ExperimentalApi, apiLintMode)
    if (includeRestrictedApis) {
        generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
            apiLocation.restrictedApiFile, tempDir, GenerateApiMode.RestrictedApi, ApiLintMode.Skip)
    }
}

// Generates the specified api file
fun Project.generateApi(
    bootClasspath: Collection<File>,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputFile: File,
    tempDir: File,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode
) {
    val tempOutputFile = if (generateApiMode is GenerateApiMode.RestrictedApi) {
        File(tempDir, outputFile.name + ".tmp")
    } else {
        outputFile
    }

    // generate public API txt
    val args = mutableListOf(
        "--classpath",
        (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

        "--source-path",
        sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

        "--api",
        tempOutputFile.toString(),

        "--format=v3",
        "--output-kotlin-nulls=yes"
    )

    when (generateApiMode) {
        is GenerateApiMode.PublicApi -> {
            args += HIDE_EXPERIMENTAL_ARGS
        }
        is GenerateApiMode.RestrictedApi -> {
            // Show restricted APIs despite @hide.
            args += listOf(
                "--show-annotation", "androidx.annotation.RestrictTo",
                "--show-unannotated"
            )
            args += HIDE_EXPERIMENTAL_ARGS
        }
        is GenerateApiMode.ExperimentalApi -> {
            // No additional args needed.
        }
    }

    when (apiLintMode) {
        is ApiLintMode.CheckBaseline -> {
            args += API_LINT_ARGS
            if (apiLintMode.apiLintBaseline.exists()) {
                args += listOf("--baseline", apiLintMode.apiLintBaseline.toString())
            }
            args.addAll(listOf(
                "--error",
                "DeprecationMismatch" // Enforce deprecation mismatch
            ))
        }
        is ApiLintMode.Skip -> {
            args.addAll(listOf(
                "--hide",
                "DeprecationMismatch",
                "--hide",
                "UnhiddenSystemApi",
                "--hide",
                "ReferencesHidden"
            ))
        }
    }

    val metalavaConfiguration = getMetalavaConfiguration()
    runMetalavaWithArgs(metalavaConfiguration, args)

    if (generateApiMode is GenerateApiMode.RestrictedApi) {
        removeRestrictToLibraryLines(tempOutputFile, outputFile)
    }
}

// until b/119617147 is done, remove lines containing "@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)"
private fun removeRestrictToLibraryLines(inputFile: File, outputFile: File) {
    val outputBuilder = StringBuilder()
    val lines = inputFile.readLines()
    var skipScopeUntil: String? = null
    for (line in lines) {
        val skip = line.contains("@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
        if (skip && line.endsWith("{")) {
            skipScopeUntil = line.commonPrefixWith("    ") + "}"
        }
        if (!skip && skipScopeUntil == null) {
            outputBuilder.append(line)
            outputBuilder.append("\n")
        }
        if (line == skipScopeUntil) {
            skipScopeUntil = null
        }
    }
    if (skipScopeUntil != null) {
        throw GradleException("Skipping until `$skipScopeUntil`, but found EOF")
    }
    outputFile.writeText(outputBuilder.toString())
}
