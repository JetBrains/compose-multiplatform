/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build

import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.BuilderConstants
import net.ltgt.gradle.errorprone.ErrorProneBasePlugin
import net.ltgt.gradle.errorprone.ErrorProneToolChain
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply

const val ERROR_PRONE_TASK = "runErrorProne"

private const val ERROR_PRONE_VERSION = "com.google.errorprone:error_prone_core:2.3.2"
private val log = Logging.getLogger("ErrorProneConfiguration")

fun Project.configureErrorProneForJava() {
    val toolChain = createErrorProneToolChain()
    val javaCompileProvider = project.tasks.named(COMPILE_JAVA_TASK_NAME, JavaCompile::class.java)
    log.info("Configuring error-prone for ${project.path}")
    makeErrorProneTask(javaCompileProvider, toolChain)
}

fun Project.configureErrorProneForAndroid(variants: DomainObjectSet<out BaseVariant>) {
    val toolChain = createErrorProneToolChain()
    variants.all { variant ->
        if (variant.buildType.name == BuilderConstants.DEBUG) {
            val task = variant.javaCompileProvider

            log.info("Configuring error-prone for ${variant.name}'s java compile")
            makeErrorProneTask(task, toolChain)
        }
    }
}

private fun Project.createErrorProneToolChain(): ErrorProneToolChain {
    apply<ErrorProneBasePlugin>()

    val toolChain = ErrorProneToolChain.create(this)
    // Pin a specific version of the compiler. By default a dependency wildcard is used.
    dependencies.add(ErrorProneBasePlugin.CONFIGURATION_NAME, ERROR_PRONE_VERSION)
    return toolChain
}

// Given an existing JavaCompile task, reconfigures the task to use the ErrorProne compiler
private fun JavaCompile.configureWithErrorProne(toolChain: ErrorProneToolChain) {
    this.toolChain = toolChain

    val compilerArgs = this.options.compilerArgs
    compilerArgs += listOf(
            "-XDcompilePolicy=simple", // Workaround for b/36098770
            "-XepExcludedPaths:.*/(build/generated|build/errorProne|external)/.*",

            // Disable the following checks.
            "-Xep:RestrictTo:OFF",
            "-Xep:ObjectToString:OFF",
            "-Xep:CatchAndPrintStackTrace:OFF",

            // Enforce the following checks.
            "-Xep:ParameterNotNullable:ERROR",
            "-Xep:MissingOverride:ERROR",
            "-Xep:JdkObsolete:ERROR",
            "-Xep:EqualsHashCode:ERROR",
            "-Xep:NarrowingCompoundAssignment:ERROR",
            "-Xep:ClassNewInstance:ERROR",
            "-Xep:ClassCanBeStatic:ERROR",
            "-Xep:SynchronizeOnNonFinalField:ERROR",
            "-Xep:OperatorPrecedence:ERROR",
            "-Xep:IntLongMath:ERROR",
            "-Xep:MissingFail:ERROR",
            "-Xep:JavaLangClash:ERROR",
            "-Xep:PrivateConstructorForUtilityClass:ERROR",
            "-Xep:TypeParameterUnusedInFormals:ERROR",
            "-Xep:StringSplitter:ERROR",
            "-Xep:ReferenceEquality:ERROR",
            "-Xep:AssertionFailureIgnored:ERROR",

            // Nullaway
            "-XepIgnoreUnknownCheckNames", // https://github.com/uber/NullAway/issues/25
            "-Xep:NullAway:ERROR",
            "-XepOpt:NullAway:AnnotatedPackages=android.arch,android.support,androidx"
    )
}

/**
 * Given a [JavaCompile] task, creates a task that runs the ErrorProne compiler with the same
 * settings.
 */
private fun Project.makeErrorProneTask(
    compileTaskProvider: TaskProvider<JavaCompile>,
    toolChain: ErrorProneToolChain
) {
    val provider = maybeRegister<JavaCompile>(
        name = ERROR_PRONE_TASK,
        onConfigure = {
            val compileTask = compileTaskProvider.get()
            it.classpath = compileTask.classpath

            it.source = compileTask.source
            it.destinationDir = file(buildDir.resolve("errorProne"))
            it.options.compilerArgs = compileTask.options.compilerArgs.toMutableList()
            it.options.annotationProcessorPath = compileTask.options.annotationProcessorPath
            it.options.bootstrapClasspath = compileTask.options.bootstrapClasspath
            it.sourceCompatibility = compileTask.sourceCompatibility
            it.targetCompatibility = compileTask.targetCompatibility
            it.configureWithErrorProne(toolChain)
            it.dependsOn(compileTask.dependsOn)
        },
        onRegister = { errorProneProvider ->
            tasks.named("check").configure {
                it.dependsOn(errorProneProvider)
            }
        }
    )
}
