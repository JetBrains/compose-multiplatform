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
import androidx.build.getLibraryByName
import androidx.build.java.JavaCompileInputs
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

// MetalavaRunner stores common configuration for executing Metalava

fun runMetalavaWithArgs(
    metalavaClasspath: FileCollection,
    args: List<String>,
    workerExecutor: WorkerExecutor
) {
    val allArgs = listOf(
        "--no-banner",
        "--hide",
        "HiddenSuperclass", // We allow having a hidden parent class

        "--error",
        "UnresolvedImport",

        "--delete-empty-removed-signatures"
    ) + args
    val workQueue = workerExecutor.processIsolation()
    workQueue.submit(MetalavaWorkAction::class.java) { parameters ->
        parameters.args.set(allArgs)
        parameters.metalavaClasspath.set(metalavaClasspath.files)
    }
}

interface MetalavaParams : WorkParameters {
    val args: ListProperty<String>
    val metalavaClasspath: SetProperty<File>
}

abstract class MetalavaWorkAction @Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<MetalavaParams> {
    override fun execute() {
        val outputStream = ByteArrayOutputStream()
        var successful = false
        try {
            execOperations.javaexec {
                // Intellij core reflects into java.util.ResourceBundle
                it.jvmArgs = listOf(
                    "--add-opens",
                    "java.base/java.util=ALL-UNNAMED"
                )
                it.classpath(parameters.metalavaClasspath.get())
                it.mainClass.set("com.android.tools.metalava.Driver")
                it.args = parameters.args.get()
                it.setStandardOutput(outputStream)
                it.setErrorOutput(outputStream)
            }
            successful = true
        } finally {
            if (!successful) {
                System.err.println(outputStream.toString(Charsets.UTF_8))
            }
        }
    }
}

fun Project.getMetalavaClasspath(): FileCollection {
    val configuration = configurations.findByName("metalava") ?: configurations.create("metalava") {
        it.dependencies.add(dependencies.create(getLibraryByName("metalava")))
    }
    return project.files(configuration)
}

// Metalava arguments to hide all experimental API surfaces.
val HIDE_EXPERIMENTAL_ARGS: List<String> = listOf(
    "--hide-annotation", "androidx.annotation.experimental.Experimental",
    "--hide-annotation", "kotlin.Experimental",
    "--hide-annotation", "androidx.annotation.RequiresOptIn",
    "--hide-annotation", "kotlin.RequiresOptIn",
    "--hide-meta-annotation", "androidx.annotation.experimental.Experimental",
    "--hide-meta-annotation", "kotlin.Experimental",
    "--hide-meta-annotation", "androidx.annotation.RequiresOptIn",
    "--hide-meta-annotation", "kotlin.RequiresOptIn",
)

fun getApiLintArgs(targetsJavaConsumers: Boolean): List<String> {
    val args = mutableListOf(
        "--api-lint",
        "--hide",
        listOf(
            // The list of checks that are hidden as they are not useful in androidx
            "Enum", // Enums are allowed to be use in androidx
            "CallbackInterface", // With target Java 8, we have default methods
            "ProtectedMember", // We allow using protected members in androidx
            "ManagerLookup", // Managers in androidx are not the same as platform services
            "ManagerConstructor",
            "RethrowRemoteException", // This check is for calls into system_server
            "PackageLayering", // This check is not relevant to androidx.* code.
            "UserHandle", // This check is not relevant to androidx.* code.
            "ParcelableList", // This check is only relevant to android platform that has managers.

            // List of checks that have bugs, but should be enabled once fixed.
            "StaticUtils", // b/135489083
            "StartWithLower", // b/135710527

            // The list of checks that are API lint warnings and are yet to be enabled
            "SamShouldBeLast",

            // We should only treat these as warnings
            "IntentBuilderName",
            "OnNameExpected",
            "UserHandleName"
        ).joinToString(),
        "--error",
        listOf(
            "AllUpper",
            "GetterSetterNames",
            "MinMaxConstant",
            "TopLevelBuilder",
            "BuilderSetStyle",
            "MissingBuildMethod",
            "SetterReturnsThis",
            "OverlappingConstants",
            "IllegalStateException",
            "ListenerLast",
            "ExecutorRegistration",
            "StreamFiles",
            "AbstractInner",
            "NotCloseable",
            "MethodNameTense",
            "UseIcu",
            "NoByteOrShort",
            "CommonArgsFirst",
            "GetterOnBuilder",
            "CallbackMethodName",
            "StaticFinalBuilder",
            "MissingGetterMatchingBuilder",
            "HiddenSuperclass",
            "KotlinOperator"
        ).joinToString()
    )
    if (targetsJavaConsumers) {
        args.addAll(listOf("--error", "MissingJvmstatic", "--error", "ArrayReturn"))
    } else {
        args.addAll(listOf("--hide", "MissingJvmstatic", "--hide", "ArrayReturn"))
    }
    return args
}

sealed class GenerateApiMode {
    object PublicApi : GenerateApiMode()
    object AllRestrictedApis : GenerateApiMode()
    object RestrictToLibraryGroupPrefixApis : GenerateApiMode()
    object ExperimentalApi : GenerateApiMode()
}

sealed class ApiLintMode {
    class CheckBaseline(
        val apiLintBaseline: File,
        val targetsJavaConsumers: Boolean
    ) : ApiLintMode()
    object Skip : ApiLintMode()
}

// Generates all of the specified api files
fun generateApi(
    metalavaClasspath: FileCollection,
    files: JavaCompileInputs,
    apiLocation: ApiLocation,
    apiLintMode: ApiLintMode,
    includeRestrictToLibraryGroupApis: Boolean,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null
) {
    // API lint runs on the experimental pass, which also includes public API. This means API lint
    // can safely be skipped on the public pass.
    generateApi(
        metalavaClasspath, files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation, GenerateApiMode.PublicApi, ApiLintMode.Skip, workerExecutor, pathToManifest
    )
    generateApi(
        metalavaClasspath, files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation, GenerateApiMode.ExperimentalApi, apiLintMode, workerExecutor, pathToManifest
    )

    val restrictedAPIMode = if (includeRestrictToLibraryGroupApis) {
        GenerateApiMode.AllRestrictedApis
    } else {
        GenerateApiMode.RestrictToLibraryGroupPrefixApis
    }
    generateApi(
        metalavaClasspath, files.bootClasspath, files.dependencyClasspath, files.sourcePaths.files,
        apiLocation, restrictedAPIMode, ApiLintMode.Skip, workerExecutor
    )
}

// Gets arguments for generating the specified api file
private fun generateApi(
    metalavaClasspath: FileCollection,
    bootClasspath: FileCollection,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputLocation: ApiLocation,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    workerExecutor: WorkerExecutor,
    pathToManifest: String? = null
) {
    val args = getGenerateApiArgs(
        bootClasspath, dependencyClasspath, sourcePaths, outputLocation,
        generateApiMode, apiLintMode, pathToManifest
    )
    runMetalavaWithArgs(metalavaClasspath, args, workerExecutor)
}

// Generates the specified api file
fun getGenerateApiArgs(
    bootClasspath: FileCollection,
    dependencyClasspath: FileCollection,
    sourcePaths: Collection<File>,
    outputLocation: ApiLocation?,
    generateApiMode: GenerateApiMode,
    apiLintMode: ApiLintMode,
    pathToManifest: String? = null
): List<String> {
    // generate public API txt
    val args = mutableListOf(
        "--classpath",
        (bootClasspath.files + dependencyClasspath.files).joinToString(File.pathSeparator),

        "--source-path",
        sourcePaths.filter { it.exists() }.joinToString(File.pathSeparator),

        "--format=v4",
        "--output-kotlin-nulls=yes",
        "--warnings-as-errors"
    )

    pathToManifest?.let {
        args += listOf("--manifest", pathToManifest)
    }

    if (outputLocation != null) {
        when (generateApiMode) {
            is GenerateApiMode.PublicApi -> {
                args += listOf("--api", outputLocation.publicApiFile.toString())
                args += listOf("--removed-api", outputLocation.removedApiFile.toString())
            }
            is GenerateApiMode.AllRestrictedApis,
            GenerateApiMode.RestrictToLibraryGroupPrefixApis -> {
                args += listOf("--api", outputLocation.restrictedApiFile.toString())
            }
            is GenerateApiMode.ExperimentalApi -> {
                args += listOf("--api", outputLocation.experimentalApiFile.toString())
            }
        }
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
            // Despite being hidden we still track the following:
            // * @RestrictTo(Scope.LIBRARY_GROUP_PREFIX): inter-library APIs
            // * @PublishedApi: needs binary stability for inline methods
            // * @RestrictTo(Scope.LIBRARY_GROUP): APIs between libraries in non-atomic groups
            args += listOf(
                // hide RestrictTo(LIBRARY), use --show-annotation for RestrictTo with
                // specific arguments
                "--hide-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)",
                "--show-annotation",
                "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                    "LIBRARY_GROUP_PREFIX)",
                "--show-annotation",
                "kotlin.PublishedApi",
                "--show-unannotated"
            )
            if (generateApiMode is GenerateApiMode.AllRestrictedApis) {
                args += listOf(
                    "--show-annotation",
                    "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                        "LIBRARY_GROUP)"
                )
            } else {
                args += listOf(
                    "--hide-annotation",
                    "androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope." +
                        "LIBRARY_GROUP)"
                )
            }
            args += HIDE_EXPERIMENTAL_ARGS
        }
        is GenerateApiMode.ExperimentalApi -> {
            args += listOf(
                "--hide-annotation", "androidx.annotation.RestrictTo"
            )
            args += listOf("--show-unannotated")
        }
    }

    when (apiLintMode) {
        is ApiLintMode.CheckBaseline -> {
            args += getApiLintArgs(apiLintMode.targetsJavaConsumers)
            if (apiLintMode.apiLintBaseline.exists()) {
                args += listOf("--baseline", apiLintMode.apiLintBaseline.toString())
            }
            args.addAll(
                listOf(
                    "--error",
                    "DeprecationMismatch", // Enforce deprecation mismatch
                    "--error",
                    "ReferencesDeprecated",
                    "--error-message:api-lint",
                    """
    ${TERMINAL_RED}Your change has API lint issues. Fix the code according to the messages above.$TERMINAL_RESET

    If a check is broken, suppress it in code in Kotlin with @Suppress("id")/@get:Suppress("id")
    and in Java with @SuppressWarnings("id") and file bug to
    https://issuetracker.google.com/issues/new?component=739152&template=1344623

    If you are doing a refactoring or suppression above does not work, use ./gradlew updateApiLintBaseline
"""
                )
            )
        }
        is ApiLintMode.Skip -> {
            args.addAll(
                listOf(
                    "--hide",
                    "DeprecationMismatch",
                    "--hide",
                    "UnhiddenSystemApi",
                    "--hide",
                    "ReferencesHidden",
                    "--hide",
                    "ReferencesDeprecated"
                )
            )
        }
    }

    return args
}
