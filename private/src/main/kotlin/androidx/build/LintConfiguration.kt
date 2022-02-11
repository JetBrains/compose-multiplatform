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

package androidx.build

import androidx.build.dependencyTracker.AffectedModuleDetector
import com.android.build.api.dsl.Lint
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.AndroidLintTextOutputTask
import com.google.common.io.Files
import java.io.File
import java.util.Locale
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType

/**
 * Setting this property means that lint will update lint-baseline.xml if it exists.
 */
private const val UPDATE_LINT_BASELINE = "updateLintBaseline"

/**
 * Name of the service we use to limit the number of concurrent executions of lint
 */
public const val LINT_SERVICE_NAME = "androidxLintService"

/**
 * Property used by Lint to continue creating baselines without failing lint, normally set by:
 * -Dlint.baselines.continue=true from command line.
 */
private const val LINT_BASELINE_CONTINUE = "lint.baselines.continue"

// service for limiting the number of concurrent lint tasks
interface AndroidXLintService : BuildService<BuildServiceParameters.None>

fun Project.configureRootProjectForLint() {
    // determine many lint tasks to run in parallel
    val memoryPerTask = 512 * 1024 * 1024
    val maxLintMemory = Runtime.getRuntime().maxMemory() * 0.75 // save memory for other things too
    val maxNumParallelUsages = Math.max(1, (maxLintMemory / memoryPerTask).toInt())

    project.gradle.sharedServices.registerIfAbsent(
        LINT_SERVICE_NAME,
        AndroidXLintService::class.java
    ) { spec ->
        spec.maxParallelUsages.set(maxNumParallelUsages)
    }
}

fun Project.configureNonAndroidProjectForLint(extension: AndroidXExtension) {
    apply(mapOf("plugin" to "com.android.lint"))

    // Create fake variant tasks since that is what is invoked by developers.
    val lintTask = tasks.named("lint")
    lintTask.configure { task ->
        AffectedModuleDetector.configureTaskGuard(task)
    }
    afterEvaluate {
        tasks.named("lintAnalyze").configure { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }
        /* TODO: uncomment when we upgrade to AGP 7.1.0-alpha04
        tasks.named("lintReport").configure { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }*/
    }
    tasks.register("lintDebug") {
        it.dependsOn(lintTask)
        it.enabled = false
    }
    tasks.register("lintAnalyzeDebug") {
        it.enabled = false
    }
    tasks.register("lintRelease") {
        it.dependsOn(lintTask)
        it.enabled = false
    }
    addToBuildOnServer(lintTask)

    val lint = extensions.getByType<Lint>()
    // Support the lint standalone plugin case which, as yet, lacks AndroidComponents finalizeDsl
    afterEvaluate { configureLint(lint, extension) }
}

fun Project.configureAndroidProjectForLint(lint: Lint, extension: AndroidXExtension) {
    project.afterEvaluate {
        // makes sure that the lintDebug task will exist, so we can find it by name
        setUpLintDebugIfNeeded()
    }
    tasks.register("lintAnalyze") {
        it.enabled = false
    }
    configureLint(lint, extension)
    tasks.named("lint").configure { task ->
        // We already run lintDebug, we don't need to run lint which lints the release variant
        task.enabled = false
    }
    afterEvaluate {
        for (variant in project.agpVariants) {
            tasks.named(
                "lint${variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }}"
            ).configure { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }
            tasks.named(
                "lintAnalyze${variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }}"
            ).configure { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }
            /* TODO: uncomment when we upgrade to AGP 7.1.0-alpha04
            tasks.named("lintReport${variant.name.capitalize(Locale.US)}").configure { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }*/
        }
    }
}

private fun Project.setUpLintDebugIfNeeded() {
    val variants = project.agpVariants
    val variantNames = variants.map { v -> v.name }
    if (!variantNames.contains("debug")) {
        tasks.register("lintDebug") {
            for (variantName in variantNames) {
                if (variantName.lowercase(Locale.US).contains("debug")) {
                    it.dependsOn(
                        tasks.named(
                            "lint${variantName.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                            }}"
                        )
                    )
                }
            }
        }
    }
}

fun Project.configureLint(lint: Lint, extension: AndroidXExtension) {
    project.dependencies.add(
        "lintChecks",
        project.rootProject.project(":lint-checks")
    )

    // The purpose of this specific project is to test that lint is running, so
    // it contains expected violations that we do not want to trigger a build failure
    val isTestingLintItself = (project.path == ":lint-checks:integration-tests")

    // If -PupdateLintBaseline was set we should update the baseline if it exists
    val updateLintBaseline = project.providers.gradleProperty(UPDATE_LINT_BASELINE).isPresent &&
        !isTestingLintItself

    lint.apply {
        // Skip lintVital tasks on assemble. We explicitly run lintRelease for libraries.
        checkReleaseBuilds = false
    }

    // task to copy autogenerated baselines back into the source tree
    val updateBaselineTask = project.tasks.register(
        "updateLintBaseline",
        UpdateBaselineTask::class.java,
    ) { copyTask ->
        copyTask.source.set(generatedLintBaseline)
        copyTask.dest.set(lintBaseline)
    }

    val projectGeneratedLintBaseline = generatedLintBaseline

    tasks.withType(AndroidLintAnalysisTask::class.java).configureEach { task ->
        // don't run too many copies of lint at once due to memory limitations
        task.usesService(
            task.project.gradle.sharedServices.registrations.getByName(LINT_SERVICE_NAME).service
        )
        if (updateLintBaseline) {
            // Remove the generated baseline before running lint, so that lint won't try to use it
            task.doFirst {
                if (projectGeneratedLintBaseline.exists()) {
                    projectGeneratedLintBaseline.delete()
                }
            }
        }
    }
    tasks.withType(AndroidLintTextOutputTask::class.java).configureEach { task ->
        if (updateLintBaseline) {
            task.finalizedBy(updateBaselineTask)
        }
    }

    // Lint is configured entirely in finalizeDsl so that individual projects cannot easily
    // disable individual checks in the DSL for any reason.
    lint.apply {
        if (!isTestingLintItself) {
            abortOnError = true
        }
        ignoreWarnings = true

        // Run lint on tests. Uses top-level lint.xml to specify checks.
        checkTestSources = true

        // Write output directly to the console (and nowhere else).
        textReport = true
        htmlReport = false

        // Format output for convenience.
        explainIssues = true
        noLines = false
        quiet = true

        // We run lint on each library, so we don't want transitive checking of each dependency
        checkDependencies = false

        if (
            extension.type == LibraryType.PUBLISHED_TEST_LIBRARY ||
            extension.type == LibraryType.INTERNAL_TEST_LIBRARY
        ) {
            // Test libraries are allowed to call @VisibleForTests code
            disable.add("VisibleForTests")
        } else {
            fatal.add("VisibleForTests")
        }

        // Disable dependency checks that suggest to change them. We want libraries to be
        // intentional with their dependency version bumps.
        disable.add("KtxExtensionAvailable")
        disable.add("GradleDependency")

        // Disable a check that's only relevant for real apps. For our test apps we're not
        // concerned with drawables potentially being a little bit blurry
        disable.add("IconMissingDensityFolder")

        // Disable a check that's only triggered by translation updates which are
        // outside of library owners' control, b/174655193
        disable.add("UnusedQuantity")

        // Disable until it works for our projects, b/171986505
        disable.add("JavaPluginLanguageLevel")

        // Disable the TODO check until we have a policy that requires it.
        disable.add("StopShip")

        // Broken in 7.0.0-alpha15 due to b/180408990
        disable.add("RestrictedApi")

        // Broken in 7.0.0-alpha15 due to b/187508590
        disable.add("InvalidPackage")

        // Reenable after upgradingto 7.1.0-beta01
        disable.add("SupportAnnotationUsage")

        // Provide stricter enforcement for project types intended to run on a device.
        if (extension.type.compilationTarget == CompilationTarget.DEVICE) {
            fatal.add("Assert")
            fatal.add("NewApi")
            fatal.add("ObsoleteSdkInt")
            fatal.add("NoHardKeywords")
            fatal.add("UnusedResources")
            fatal.add("KotlinPropertyAccess")
            fatal.add("LambdaLast")
            fatal.add("UnknownNullness")

            // Only override if not set explicitly.
            // Some Kotlin projects may wish to disable this.
            if (
                !disable.contains("SyntheticAccessor") &&
                extension.type != LibraryType.SAMPLES
            ) {
                fatal.add("SyntheticAccessor")
            }

            // Only check for missing translations in finalized (beta and later) modules.
            if (extension.mavenVersion?.isFinalApi() == true) {
                fatal.add("MissingTranslation")
            } else {
                disable.add("MissingTranslation")
            }
        } else {
            disable.add("BanUncheckedReflection")
        }

        // Broken in 7.0.0-alpha15 due to b/187343720
        disable.add("UnusedResources")

        if (extension.type == LibraryType.SAMPLES) {
            // TODO: b/190833328 remove if / when AGP will analyze dependencies by default
            //  This is needed because SampledAnnotationDetector uses partial analysis, and
            //  hence requires dependencies to be analyzed.
            checkDependencies = true
        }

        // Only run certain checks where API tracking is important.
        if (extension.type.checkApi is RunApiTasks.No) {
            disable.add("IllegalExperimentalApiUsage")
        }

        // If the project has not overridden the lint config, set the default one.
        if (lintConfig == null) {
            val lintXmlPath = if (extension.type == LibraryType.SAMPLES) {
                "buildSrc/lint_samples.xml"
            } else {
                "buildSrc/lint.xml"
            }
            // suppress warnings more specifically than issue-wide severity (regexes)
            // Currently suppresses warnings from baseline files working as intended
            lintConfig = File(project.getSupportRootFolder(), lintXmlPath)
        }

        // Ideally, teams aren't able to add new violations to a baseline file; they should only
        // be able to burn down existing violations. That's hard to enforce, though, so we'll
        // generally allow teams to update their baseline files with a publicly-known flag.
        if (updateLintBaseline) {
            // Continue generating baselines regardless of errors.
            abortOnError = false

            // Avoid printing every single lint error to the terminal.
            textReport = false
        }

        val lintBaselineFile = lintBaseline.get().asFile
        // If we give lint the filepath of a baseline file, then:
        //   If the file does not exist, lint will write new violations to it
        //   If the file does exist, lint will read extemptions from it

        // So, if we want to update the baselines, we need to give lint an empty location
        // to save to.

        // If we're not updating the baselines, then we want lint to check for new errors.
        // This requires us only pass a baseline to lint if one already exists.
        if (updateLintBaseline) {
            baseline = generatedLintBaseline
        } else if (lintBaselineFile.exists()) {
            baseline = lintBaselineFile
        }
    }
}

val Project.lintBaseline get() =
    project.objects.fileProperty().fileValue(File(projectDir, "/lint-baseline.xml"))
val Project.generatedLintBaseline get() = File(project.buildDir, "/generated-lint-baseline.xml")

/**
 * Task that copies the generated line baseline
 * If an input baseline file has no issues, it is considered to be nonexistent
 */
abstract class UpdateBaselineTask : DefaultTask() {
    @get:InputFiles // allows missing input file
    abstract val source: RegularFileProperty

    @get:OutputFile
    abstract val dest: RegularFileProperty

    @TaskAction
    fun copyBaseline() {
        val source = source.get().asFile
        val dest = dest.get().asFile
        val sourceText = if (source.exists()) {
            source.readText()
        } else {
            ""
        }
        var sourceHasIssues =
            if (source.exists()) {
                // Does the baseline contain any issues?
                source.reader().useLines { lines ->
                    lines.any { line ->
                        line.endsWith("<issue")
                    }
                }
            } else {
                false
            }
        val destNonempty = dest.exists()
        val changing = (sourceHasIssues != destNonempty) ||
            (sourceHasIssues && sourceText != dest.readText())
        if (changing) {
            if (sourceHasIssues) {
                Files.copy(source, dest)
                println("Updated baseline file ${dest.path}")
            } else {
                dest.delete()
                println("Deleted baseline file ${dest.path}")
            }
        }
    }
}
