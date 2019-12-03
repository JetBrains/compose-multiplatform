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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

// MetalavaRunner stores common configuration for executing Metalava

fun runMetalavaWithArgs(
    metalavaConfiguration: Configuration,
    args: List<String>,
    workerExecutor: WorkerExecutor
) {
    val allArgs = listOf(
        "--no-banner",
        "--hide",
        "HiddenSuperclass" // We allow having a hidden parent class
    ) + args

    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(MetalavaWorkAction::class.java) { parameters ->
        parameters.getArgs().set(allArgs)
        parameters.getMetalavaClasspath().set(metalavaConfiguration.files)
    }
}

interface MetalavaParams : WorkParameters {
    fun getArgs(): ListProperty<String>
    fun getMetalavaClasspath(): SetProperty<File>
}

abstract class MetalavaWorkAction @Inject constructor (
    private val execOperations: ExecOperations
) : WorkAction<MetalavaParams> {

    override fun execute() {
        val allArgs = getParameters().getArgs().get()
        val metalavaJar = getParameters().getMetalavaClasspath().get()

        execOperations.javaexec {
            it.classpath(metalavaJar)
            it.main = "com.android.tools.metalava.Driver"
            it.args = allArgs
        }
    }
}

fun Project.getMetalavaConfiguration(): Configuration {
    return configurations.findByName("metalava") ?: configurations.create("metalava") {
        val dependency = dependencies.create("com.android:metalava:1.3.0")
        it.dependencies.add(dependency)
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
        "ParcelableList", // This check is only relevant to android platform that has managers.

        // List of checks that have bugs, but should be enabled once fixed.
        "GetterSetterNames", // b/135498039
        "StaticUtils", // b/135489083
        "AllUpper", // b/135708486
        "StartWithLower", // b/135710527

        // The list of checks that are API lint warnings and are yet to be enabled
        "ExecutorRegistration",
        "SamShouldBeLast",
        "MissingJvmstatic",
        "CallbackMethodName",
        "GetterOnBuilder",
        "StaticFinalBuilder",
        "MissingGetterMatchingBuilder",

        // We should only treat these as warnings
        "IntentBuilderName",
        "OnNameExpected",
        "UserHandleName"
    ).joinToString(),
    "--error",
    listOf(
        "MinMaxConstant",
        "TopLevelBuilder",
        "BuilderSetStyle",
        "MissingBuildMethod",
        "SetterReturnsThis",
        "OverlappingConstants",
        "IllegalStateException",
        "ListenerLast",
        "StreamFiles",
        "AbstractInner",
        "NotCloseable",
        "ArrayReturn",
        "MethodNameTense",
        "UseIcu",
        "NoByteOrShort",
        "CommonArgsFirst"
    ).joinToString()
)

sealed class GenerateApiMode {
    object PublicApi : GenerateApiMode()
    object AllRestrictedApis : GenerateApiMode()
    object RestrictToLibraryGroupPrefixApis : GenerateApiMode()
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
    apiLintMode: ApiLintMode,
    includeRestrictToLibraryGroupApis: Boolean,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null
) {
    generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation.publicApiFile, GenerateApiMode.PublicApi, apiLintMode, workerExecutor,
        pathToManifest)
    generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation.experimentalApiFile, GenerateApiMode.ExperimentalApi, apiLintMode,
        workerExecutor, pathToManifest)

    val restrictedAPIMode = if (includeRestrictToLibraryGroupApis) {
        GenerateApiMode.AllRestrictedApis
    } else {
        GenerateApiMode.RestrictToLibraryGroupPrefixApis
    }
    generateApi(files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation.restrictedApiFile, restrictedAPIMode, ApiLintMode.Skip,
        workerExecutor)
}

// Gets arguments for generating the specified api file
fun Project.generateApi(
    bootClasspath: Collection<File>,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputFile: File,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null
) {
    val args = getGenerateApiArgs(bootClasspath, dependencyClasspath, sourcePaths, outputFile,
        generateApiMode, apiLintMode, pathToManifest)
    runMetalavaWithArgs(getMetalavaConfiguration(), args, workerExecutor)
}

// Generates the specified api file
fun Project.getGenerateApiArgs(
    bootClasspath: Collection<File>,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputFile: File?,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    pathToManifest: String? = null
): List<String> {
    // generate public API txt
    val args = mutableListOf(
        "--classpath",
        (bootClasspath + dependencyClasspath.files).joinToString(File.pathSeparator),

        "--source-path",
        sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

        "--format=v3",
        "--output-kotlin-nulls=yes"
    )

    pathToManifest?.let {
        args += listOf("--manifest", pathToManifest)
    }

    if (outputFile != null) {
        args += listOf("--api", outputFile.toString())
    }

    when (generateApiMode) {
        is GenerateApiMode.PublicApi -> {
            args += HIDE_EXPERIMENTAL_ARGS
            args += listOf(
                "--hide-annotation", "androidx.annotation.RestrictTo"
            )
            args += listOf("--show-unannotated")
        }
        is GenerateApiMode.AllRestrictedApis, GenerateApiMode.RestrictToLibraryGroupPrefixApis -> {
            // Show restricted APIs despite @hide.
            args += listOf(
                "--show-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP_PREFIX)",
                "--show-annotation",
                "kotlin.PublishedApi",
                "--show-unannotated"
            )
            if (generateApiMode is GenerateApiMode.AllRestrictedApis) {
                args += listOf("--show-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP)")
            } else {
                args += listOf("--hide-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP)")
            }
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
                "DeprecationMismatch", // Enforce deprecation mismatch
                "--error",
                "ReferencesDeprecated"
            ))
        }
        is ApiLintMode.Skip -> {
            args.addAll(listOf(
                "--hide",
                "DeprecationMismatch",
                "--hide",
                "UnhiddenSystemApi",
                "--hide",
                "ReferencesHidden",
                "--hide",
                "ReferencesDeprecated"
            ))
        }
    }

    return args
}
