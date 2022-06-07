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

import androidx.build.java.JavaCompileInputs
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.builder.core.BuilderConstants
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.process.CommandLineArgumentProvider

const val ERROR_PRONE_TASK = "runErrorProne"

private const val ERROR_PRONE_CONFIGURATION = "errorprone"
private const val ERROR_PRONE_VERSION = "com.google.errorprone:error_prone_core:2.4.0"
private val log = Logging.getLogger("ErrorProneConfiguration")

fun Project.configureErrorProneForJava() {
    val errorProneConfiguration = createErrorProneConfiguration()
    project.extensions.getByName<SourceSetContainer>("sourceSets").configureEach {
        project.configurations[it.annotationProcessorConfigurationName].extendsFrom(
            errorProneConfiguration
        )
    }
    val javaCompileProvider = project.tasks.named(COMPILE_JAVA_TASK_NAME, JavaCompile::class.java)
    log.info("Configuring error-prone for ${project.path}")
    val kmpExtension = project.multiplatformExtension
    if (kmpExtension != null) {
        val jvmJarProvider = tasks.named(kmpExtension.jvm().artifactsTaskName, Jar::class.java)
        makeKmpErrorProneTask(
            javaCompileProvider,
            jvmJarProvider,
            JavaCompileInputs.fromKmpJvmTarget(project)
        )
    } else {
        makeErrorProneTask(javaCompileProvider)
    }
}

@Suppress("DEPRECATION") // BaseVariant
fun Project.configureErrorProneForAndroid(
    variants: DomainObjectSet<out com.android.build.gradle.api.BaseVariant>
) {
    var annotationArgs: MapProperty<String, String>? = null
    val extension = extensions.findByType(AndroidComponentsExtension::class.java)
    extension?.onVariants { variant ->
        annotationArgs = variant.javaCompilation.annotationProcessor.arguments
    }
    val errorProneConfiguration = createErrorProneConfiguration()
    variants.all { variant ->
        if (variant.buildType.name == BuilderConstants.RELEASE) {
            val task = variant.javaCompileProvider
            (variant as com.android.build.gradle.api.BaseVariant)
                .annotationProcessorConfiguration.extendsFrom(errorProneConfiguration)

            log.info("Configuring error-prone for ${variant.name}'s java compile")
            makeErrorProneTask(task) { javaCompile ->
                // Passing along annotation processor arguments to errorprone compile task
                javaCompile.options.compilerArgumentProviders.add(
                    CommandLineArgumentProviderAdapter(annotationArgs!!)
                )
            }
        }
    }
}

class CommandLineArgumentProviderAdapter(
    @get:Input
    val arguments: Provider<Map<String, String>>
) : CommandLineArgumentProvider {
    override fun asArguments(): MutableIterable<String> {
        return mutableListOf<String>().also {
            for ((key, value) in arguments.get()) {
                it.add("-A$key=$value")
            }
        }
    }
}

private fun Project.createErrorProneConfiguration(): Configuration {
    val errorProneConfiguration = configurations.create(ERROR_PRONE_CONFIGURATION) {
        it.isVisible = false
        it.isCanBeConsumed = false
        it.isCanBeResolved = true
        it.exclude(group = "com.google.errorprone", module = "javac")
    }
    dependencies.add(ERROR_PRONE_CONFIGURATION, ERROR_PRONE_VERSION)
    return errorProneConfiguration
}

// Given an existing JavaCompile task, reconfigures the task to use the ErrorProne compiler plugin
private fun JavaCompile.configureWithErrorProne() {
    val compilerArgs = this.options.compilerArgs
    compilerArgs += listOf(
        // Tell error-prone that we are running it on android compatible libraries
        "-XDandroidCompatible=true",

        "-XDcompilePolicy=simple", // Workaround for b/36098770
        listOf(
            "-Xplugin:ErrorProne",

            "-XepExcludedPaths:.*/(build/generated|build/errorProne|external)/.*",

            // We allow inter library RestrictTo usage.
            "-Xep:RestrictTo:OFF",

            // Disable the following checks.
            "-Xep:UnescapedEntity:OFF",
            "-Xep:MissingSummary:OFF",
            "-Xep:StaticAssignmentInConstructor:OFF",
            "-Xep:InvalidLink:OFF",
            "-Xep:InvalidInlineTag:OFF",
            "-Xep:EmptyBlockTag:OFF",
            "-Xep:EmptyCatch:OFF",
            "-Xep:JdkObsolete:OFF",
            "-Xep:PublicConstructorForAbstractClass:OFF",
            "-Xep:MutablePublicArray:OFF",
            "-Xep:NonCanonicalType:OFF",
            "-Xep:ModifyCollectionInEnhancedForLoop:OFF",
            "-Xep:InheritDoc:OFF",
            "-Xep:InvalidParam:OFF",
            "-Xep:InlineFormatString:OFF",
            "-Xep:InvalidBlockTag:OFF",
            "-Xep:ProtectedMembersInFinalClass:OFF",
            "-Xep:SameNameButDifferent:OFF",
            "-Xep:AnnotateFormatMethod:OFF",
            "-Xep:ReturnFromVoid:OFF",
            "-Xep:AlmostJavadoc:OFF",
            "-Xep:InjectScopeAnnotationOnInterfaceOrAbstractClass:OFF",
            "-Xep:InvalidThrows:OFF",

            // Disable checks which are already enforced by lint.
            "-Xep:PrivateConstructorForUtilityClass:OFF",

            // Enforce the following checks.
            "-Xep:JavaTimeDefaultTimeZone:ERROR",
            "-Xep:ParameterNotNullable:ERROR",
            "-Xep:MissingOverride:ERROR",
            "-Xep:EqualsHashCode:ERROR",
            "-Xep:NarrowingCompoundAssignment:ERROR",
            "-Xep:ClassNewInstance:ERROR",
            "-Xep:ClassCanBeStatic:ERROR",
            "-Xep:SynchronizeOnNonFinalField:ERROR",
            "-Xep:OperatorPrecedence:ERROR",
            "-Xep:IntLongMath:ERROR",
            "-Xep:MissingFail:ERROR",
            "-Xep:JavaLangClash:ERROR",
            "-Xep:TypeParameterUnusedInFormals:ERROR",
            "-Xep:StringSplitter:ERROR",
            "-Xep:ReferenceEquality:ERROR",
            "-Xep:AssertionFailureIgnored:ERROR",
            "-Xep:UnnecessaryParentheses:ERROR",
            "-Xep:EqualsGetClass:ERROR",
            "-Xep:UnusedVariable:ERROR",
            "-Xep:UnusedMethod:ERROR",
            "-Xep:UndefinedEquals:ERROR",
            "-Xep:ThreadLocalUsage:ERROR",
            "-Xep:FutureReturnValueIgnored:ERROR",
            "-Xep:ArgumentSelectionDefectChecker:ERROR",
            "-Xep:HidingField:ERROR",
            "-Xep:UnsynchronizedOverridesSynchronized:ERROR",
            "-Xep:Finally:ERROR",
            "-Xep:ThreadPriorityCheck:ERROR",
            "-Xep:AutoValueFinalMethods:ERROR",
            "-Xep:ImmutableEnumChecker:ERROR",
            "-Xep:UnsafeReflectiveConstructionCast:ERROR",
            "-Xep:LockNotBeforeTry:ERROR",
            "-Xep:DoubleCheckedLocking:ERROR",
            "-Xep:InconsistentCapitalization:ERROR",
            "-Xep:ModifiedButNotUsed:ERROR",
            "-Xep:AmbiguousMethodReference:ERROR",
            "-Xep:EqualsIncompatibleType:ERROR",
            "-Xep:ParameterName:ERROR",
            "-Xep:RxReturnValueIgnored:ERROR",
            "-Xep:BadImport:ERROR",
            "-Xep:MissingCasesInEnumSwitch:ERROR",
            "-Xep:ObjectToString:ERROR",
            "-Xep:CatchAndPrintStackTrace:ERROR",
            "-Xep:MixedMutabilityReturnType:ERROR",

            // Nullaway
            "-XepIgnoreUnknownCheckNames", // https://github.com/uber/NullAway/issues/25
            "-Xep:NullAway:ERROR",
            "-XepOpt:NullAway:AnnotatedPackages=android.arch,android.support,androidx"
        ).joinToString(" ")
    )
}

/**
 * Given a [JavaCompile] task, creates a task that runs the ErrorProne compiler with the same
 * settings, including any kotlin source provided by [jvmCompileInputs].
 *
 * Note: Since ErrorProne only understands Java files which may be dependent on Kotlin source,
 * using this method to register ErrorProne task causes it to be dependent on jvmJar task.
 *

 * @param jvmCompileInputs [JavaCompileInputs] that specifies jvm source including Kotlin sources.
 */
private fun Project.makeKmpErrorProneTask(
    compileTaskProvider: TaskProvider<JavaCompile>,
    jvmJarTaskProvider: TaskProvider<Jar>,
    jvmCompileInputs: JavaCompileInputs
) {
    makeErrorProneTask(compileTaskProvider) { errorProneTask ->
        // ErrorProne doesn't understand Kotlin source, so first let kotlinCompile finish, then
        // take the resulting jar and add it to the classpath.
        val jvmJarTask = jvmJarTaskProvider.get()
        val jvmJarFileCollection = files(
            provider { jvmJarTask.archiveFile.get().asFile }
        )
        errorProneTask.dependsOn(jvmJarTaskProvider.name)
        errorProneTask.classpath = jvmCompileInputs.dependencyClasspath.plus(jvmJarFileCollection)
        errorProneTask.source = jvmCompileInputs.sourcePaths
            // flatMap src dirs into src files so we can read the extensions.
            .asFileTree
            // ErrorProne normally skips non-java source, but we need to explicitly filter for it
            // since non-empty list with no java source will throw an exception.
            .filter { it.extension.equals("java", ignoreCase = true) }
            .asFileTree
    }
}

/**
 * Given a [JavaCompile] task, creates a task that runs the ErrorProne compiler with the same
 * settings.
 *
 * @param onConfigure optional callback which lazily evaluates on task configuration. Use this to
 * do any additional configuration such as overriding default settings.
 */
private fun Project.makeErrorProneTask(
    compileTaskProvider: TaskProvider<JavaCompile>,
    onConfigure: (errorProneTask: JavaCompile) -> Unit = { }
) {
    val errorProneTaskProvider = maybeRegister<JavaCompile>(
        name = ERROR_PRONE_TASK,
        onConfigure = {
            val compileTask = compileTaskProvider.get()
            it.classpath = compileTask.classpath
            it.source = compileTask.source
            it.destinationDirectory.set(file(buildDir.resolve("errorProne")))
            it.options.compilerArgs = compileTask.options.compilerArgs.toMutableList()
            it.options.annotationProcessorPath = compileTask.options.annotationProcessorPath
            it.options.bootstrapClasspath = compileTask.options.bootstrapClasspath
            it.sourceCompatibility = compileTask.sourceCompatibility
            it.targetCompatibility = compileTask.targetCompatibility
            it.configureWithErrorProne()
            it.dependsOn(compileTask.dependsOn)

            onConfigure(it)
        },
        onRegister = { errorProneProvider ->
            project.addToCheckTask(errorProneProvider)
        }
    )
    addToBuildOnServer(errorProneTaskProvider)
}
