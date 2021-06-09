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

package androidx.build.dependencyTracker

import androidx.build.dependencyTracker.AffectedModuleDetector.Companion.ENABLE_ARG
import androidx.build.getDistributionDirectory
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitClientImpl
import androidx.build.gradle.isRoot
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger

/**
 * The subsets we allow the projects to be partitioned into.
 * This is to allow more granular testing. Specifically, to enable running large tests on
 * CHANGED_PROJECTS, while still only running small and medium tests on DEPENDENT_PROJECTS.
 *
 * The ProjectSubset specifies which projects we are interested in testing.
 * The AffectedModuleDetector determines the minimum set of projects that must be built in
 * order to run all the tests along with their runtime dependencies.
 *
 * The subsets are:
 *  CHANGED_PROJECTS -- The containing projects for any files that were changed in this CL.
 *
 *  DEPENDENT_PROJECTS -- Any projects that have a dependency on any of the projects
 *      in the CHANGED_PROJECTS set.
 *
 *  NONE -- A status to return for a project when it is not supposed to be built.
 */
enum class ProjectSubset { DEPENDENT_PROJECTS, CHANGED_PROJECTS, NONE }

/**
 * A utility class that can discover which files are changed based on git history.
 *
 * To enable this, you need to pass [ENABLE_ARG] into the build as a command line parameter
 * (-P<name>)
 *
 * Currently, it checks git logs to find last merge CL to discover where the anchor CL is.
 *
 * Eventually, we'll move to the props passed down by the build system when it is available.
 *
 * Since this needs to check project dependency graph to work, it cannot be accessed before
 * all projects are loaded. Doing so will throw an exception.
 */
abstract class AffectedModuleDetector(
    protected val logger: Logger?
) {
    /**
     * Returns whether this project was affected by current changes.
     */
    abstract fun shouldInclude(project: Project): Boolean

    /**
     * Returns whether this task was affected by current changes.
     */
    fun shouldInclude(task: Task): Boolean {
        val include = shouldInclude(task.project)
        val inclusionVerb = if (include) "Including" else "Excluding"
        logger?.info(
            "$inclusionVerb task ${task.path}"
        )
        return include
    }

    /**
     * Returns the set that the project belongs to. The set is one of the ProjectSubset above.
     * This is used by the test config generator.
     */
    abstract fun getSubset(project: Project): ProjectSubset

    companion object {
        private const val ROOT_PROP_NAME = "affectedModuleDetector"
        private const val LOG_FILE_NAME = "affected_module_detector_log.txt"
        const val ENABLE_ARG = "androidx.enableAffectedModuleDetection"
        const val BASE_COMMIT_ARG = "androidx.affectedModuleDetector.baseCommit"

        @JvmStatic
        fun configure(gradle: Gradle, rootProject: Project) {
            val enabled = rootProject.hasProperty(ENABLE_ARG) &&
                rootProject.findProperty(ENABLE_ARG) != "false"
            if (!enabled) {
                setInstance(rootProject, AcceptAll())
                return
            }
            val logger = ToStringLogger.createWithLifecycle(gradle) { log ->
                val distDir = rootProject.getDistributionDirectory()
                distDir.let {
                    val outputFile = it.resolve(LOG_FILE_NAME)
                    outputFile.writeText(log)
                    println("wrote dependency log to ${outputFile.absolutePath}")
                }
            }
            logger.info("setup: enabled: $enabled")
            val baseCommitOverride: String? = rootProject.findProperty(BASE_COMMIT_ARG) as String?
            if (baseCommitOverride != null) {
                logger.info("using base commit override $baseCommitOverride")
            }
            gradle.taskGraph.whenReady({
                logger.lifecycle("projects evaluated")
                AffectedModuleDetectorImpl(
                    rootProject = rootProject,
                    logger = logger,
                    ignoreUnknownProjects = false,
                    baseCommitOverride = baseCommitOverride
                ).also {
                    if (!enabled) {
                        logger.info("swapping with accept all")
                        // doing it just for testing
                        setInstance(rootProject, AcceptAll(it, logger))
                    } else {
                        logger.info("using real detector")
                        setInstance(rootProject, it)
                    }
                }
            })
        }

        private fun setInstance(
            rootProject: Project,
            detector: AffectedModuleDetector
        ) {
            if (!rootProject.isRoot) {
                throw IllegalArgumentException("this should've been the root project")
            }
            rootProject.extensions.add(ROOT_PROP_NAME, detector)
        }

        private fun getInstance(project: Project): AffectedModuleDetector? {
            val extensions = project.rootProject.extensions
            return extensions.findByName(ROOT_PROP_NAME) as? AffectedModuleDetector
        }

        private fun getOrThrow(project: Project): AffectedModuleDetector {
            return getInstance(project) ?: throw GradleException(
                """
                        Tried to get affected module detector too early.
                        You cannot access it until all projects are evaluated.
                """.trimIndent()
            )
        }

        /**
         * Call this method to configure the given task to execute only if the owner project
         * is affected by current changes
         */
        @Throws(GradleException::class)
        @JvmStatic
        fun configureTaskGuard(task: Task) {
            task.onlyIf {
                getOrThrow(task.project).shouldInclude(task)
            }
        }

        /**
         * Call this method to obtain the [@link ProjectSubset] that the project is
         * determined to fall within for this particular build.
         *
         * Note that this will fail if accessed before all projects have been
         * evaluated, since the AMD does not get registered until then.
         */
        @Throws(GradleException::class)
        @JvmStatic
        internal fun getProjectSubset(project: Project): ProjectSubset {
            return getOrThrow(project).getSubset(project)
        }
    }
}

/**
 * Implementation that accepts everything without checking.
 */
private class AcceptAll(
    private val wrapped: AffectedModuleDetector? = null,
    logger: Logger? = null
) : AffectedModuleDetector(logger) {
    override fun shouldInclude(project: Project): Boolean {
        val wrappedResult = wrapped?.shouldInclude(project)
        logger?.info("[AcceptAll] wrapper returned $wrappedResult but I'll return true")
        return true
    }

    override fun getSubset(project: Project): ProjectSubset {
        val wrappedResult = wrapped?.getSubset(project)
        logger?.info("[AcceptAll] wrapper returned $wrappedResult but I'll return CHANGED_PROJECTS")
        return ProjectSubset.CHANGED_PROJECTS
    }
}

/**
 * Real implementation that checks git logs to decide what is affected.
 *
 * If any file outside a module is changed, we assume everything has changed.
 *
 * When a file in a module is changed, all modules that depend on it are considered as changed.
 */
class AffectedModuleDetectorImpl constructor(
    private val rootProject: Project,
    logger: Logger?,
    // used for debugging purposes when we want to ignore non module files
    private val ignoreUnknownProjects: Boolean = false,
    private val cobuiltTestPaths: Set<Set<String>> = COBUILT_TEST_PATHS,
    private val alwaysBuildIfExistsPaths: Set<String> = ALWAYS_BUILD_IF_EXISTS,
    private val ignoredPaths: Set<String> = IGNORED_PATHS,
    private val injectedGitClient: GitClient? = null,
    private val baseCommitOverride: String? = null
) : AffectedModuleDetector(logger) {
    private val git by lazy {
        injectedGitClient ?: GitClientImpl(rootProject.projectDir, logger)
    }

    private val dependencyTracker by lazy {
        DependencyTracker(rootProject, logger)
    }

    private val allProjects by lazy {
        rootProject.subprojects.toSet()
    }

    private val projectGraph by lazy {
        ProjectGraph(rootProject, logger)
    }

    val affectedProjects by lazy {
        changedProjects + dependentProjects
    }

    val changedProjects by lazy {
        findChangedProjects()
    }

    val dependentProjects by lazy {
        findDependentProjects()
    }

    private var unknownFiles: MutableSet<String> = mutableSetOf()

    // Files tracked by git that are not expected to effect the build, thus require no consideration
    private var ignoredFiles: MutableSet<String> = mutableSetOf()

    val buildAll by lazy {
        shouldBuildAll()
    }

    private val cobuiltTestProjects by lazy {
        lookupProjectSetsFromPaths(cobuiltTestPaths)
    }

    private val alwaysBuild by lazy {
        // For each path in alwaysBuildIfExistsPaths, if that path doesn't exist, then the developer
        // must have disabled a project that they weren't interested in using during this run.
        // Otherwise, we must always build the corresponding project during full builds.
        alwaysBuildIfExistsPaths.map { path -> rootProject.findProject(path) }.filterNotNull()
    }

    private val buildContainsNonProjectFileChanges by lazy {
        unknownFiles.isNotEmpty()
    }

    override fun shouldInclude(project: Project): Boolean {
        return if (project.isRoot || buildAll) {
            true
        } else {
            affectedProjects.contains(project)
        }
    }

    override fun getSubset(project: Project): ProjectSubset {
        return when {
            changedProjects.contains(project) -> {
                ProjectSubset.CHANGED_PROJECTS
            }
            dependentProjects.contains(project) -> {
                ProjectSubset.DEPENDENT_PROJECTS
            }
            // projects that are only included because of buildAll
            else -> {
                ProjectSubset.NONE
            }
        }
    }

    /**
     * Finds only the set of projects that were directly changed in the commit. This includes
     * placeholder-tests and any modules that need to be co-built.
     *
     * Also populates the unknownFiles var which is used in findAffectedProjects
     *
     * Returns allProjects if there are no previous merge CLs, which shouldn't happen.
     */
    private fun findChangedProjects(): Set<Project> {
        val lastMergeSha = baseCommitOverride
            ?: git.findPreviousSubmittedChange() ?: return allProjects
        val changedFiles = git.findChangedFilesSince(
            sha = lastMergeSha,
            includeUncommitted = true
        )

        val changedProjects: MutableSet<Project> = alwaysBuild.toMutableSet()

        for (filePath in changedFiles) {
            if (ignoredPaths.any {
                filePath.startsWith(it)
            }
            ) {
                ignoredFiles.add(filePath)
                logger?.info(
                    "Ignoring file: $filePath"
                )
            } else {
                val containingProject = findContainingProject(filePath)
                if (containingProject == null) {
                    unknownFiles.add(filePath)
                    logger?.info(
                        "Couldn't find containing project for file: $filePath. Adding to " +
                            "unknownFiles."
                    )
                } else {
                    changedProjects.add(containingProject)
                    logger?.info(
                        "For file $filePath containing project is $containingProject. " +
                            "Adding to changedProjects."
                    )
                }
            }
        }

        return changedProjects + getAffectedCobuiltProjects(
            changedProjects, cobuiltTestProjects
        )
    }

    /**
     * Gets all dependent projects from the set of changedProjects. This doesn't include the
     * original changedProjects. Always build is still here to ensure at least 1 thing is built
     */
    private fun findDependentProjects(): Set<Project> {
        val dependentProjects = changedProjects.flatMap {
            dependencyTracker.findAllDependents(it)
        }.toSet()
        return dependentProjects + alwaysBuild +
            getAffectedCobuiltProjects(dependentProjects, cobuiltTestProjects)
    }

    /**
     * Determines whether we are in a state where we want to build all projects, instead of
     * only affected ones. This occurs for buildSrc changes, as well as in situations where
     * we determine there are no changes within our repository (e.g. prebuilts change only)
     */
    private fun shouldBuildAll(): Boolean {
        var shouldBuildAll = false
        // Should only trigger if there are no changedFiles and no ignored files
        if (changedProjects.size == alwaysBuild.size &&
            unknownFiles.isEmpty() &&
            ignoredFiles.isEmpty()
        ) {
            shouldBuildAll = true
        } else if (unknownFiles.isNotEmpty() && !isGithubInfraChange()) {
            shouldBuildAll = true
        }
        logger?.info(
            "unknownFiles: $unknownFiles, changedProjects: $changedProjects, buildAll: " +
                "$shouldBuildAll"
        )

        if (shouldBuildAll) {
            logger?.info("Building all projects")
            if (unknownFiles.isEmpty()) {
                logger?.info("because no changed files were detected")
            } else {
                logger?.info("because one of the unknown files may affect everything in the build")
                logger?.info(
                    """
                    The modules detected as affected by changed files are
                    ${changedProjects + dependentProjects}
                    """.trimIndent()
                )
            }
        }
        return shouldBuildAll
    }

    /**
     * Returns true if all unknown changed files are contained in github setup related files.
     * (.github, playground-common). These files will not affect aosp hence should not invalidate
     * changed file tracking (e.g. not cause running all tests)
     */
    private fun isGithubInfraChange(): Boolean {
        return unknownFiles.all {
            it.contains(".github") || it.contains("playground-common")
        }
    }

    private fun lookupProjectSetsFromPaths(allSets: Set<Set<String>>): Set<Set<Project>> {
        return allSets.map { setPaths ->
            var setExists = false
            val projectSet = HashSet<Project>()
            for (path in setPaths) {
                val project = rootProject.findProject(path)
                if (project == null) {
                    if (setExists) {
                        throw IllegalStateException(
                            "One of the projects in the group of projects that are required to " +
                                "be built together is missing. Looked for " + setPaths
                        )
                    }
                } else {
                    setExists = true
                    projectSet.add(project)
                }
            }
            return@map projectSet
        }.toSet()
    }

    private fun getAffectedCobuiltProjects(
        affectedProjects: Set<Project>,
        allCobuiltSets: Set<Set<Project>>
    ): Set<Project> {
        val cobuilts = mutableSetOf<Project>()
        affectedProjects.forEach { project ->
            allCobuiltSets.forEach { cobuiltSet ->
                if (cobuiltSet.any { project == it }) {
                    cobuilts.addAll(cobuiltSet)
                }
            }
        }
        return cobuilts
    }

    private fun findContainingProject(filePath: String): Project? {
        return projectGraph.findContainingProject(filePath).also {
            logger?.info("search result for $filePath resulted in ${it?.path}")
        }
    }

    companion object {
        // Project paths that we always build if they exist
        private val ALWAYS_BUILD_IF_EXISTS = setOf(
            // placeholder test project to ensure no failure due to no instrumentation.
            // We can eventually remove if we resolve b/127819369
            ":placeholder-tests",
            ":buildSrc-tests:project-subsets"
        )

        // Some tests are codependent even if their modules are not. Enable manual bundling of tests
        private val COBUILT_TEST_PATHS = setOf(
            // Install media tests together per b/128577735
            setOf(
                // Making a change in :media:version-compat-tests makes
                // mediaGenerateTestConfiguration run (an unfortunate but low priority bug). To
                // prevent failures from missing apks, we make sure to build the
                // version-compat-tests projects in that case. Same with media2-session below.
                ":media:version-compat-tests",
                ":media:version-compat-tests:client",
                ":media:version-compat-tests:service",
                ":media:version-compat-tests:client-previous",
                ":media:version-compat-tests:service-previous"
            ),
            setOf(
                ":media2:media2-session",
                ":media2:media2-session:version-compat-tests",
                ":media2:media2-session:version-compat-tests:client",
                ":media2:media2-session:version-compat-tests:service",
                ":media2:media2-session:version-compat-tests:client-previous",
                ":media2:media2-session:version-compat-tests:service-previous"
            ), // Link graphics and material to always run @Large in presubmit per b/160624022
            setOf(
                ":compose:ui:ui-graphics",
                ":compose:material:material"
            ), // Link material and material-ripple
            setOf(
                ":compose:material:material-ripple",
                ":compose:material:material"
            ),
            setOf(
                ":benchmark:benchmark-macro",
                ":benchmark:integration-tests:macrobenchmark-target"
            ), // link benchmark-macro's correctness test and its target
            setOf(
                ":benchmark:integration-tests:macrobenchmark",
                ":benchmark:integration-tests:macrobenchmark-target"
            ), // link benchmark's macrobenchmark and its target
            setOf(
                ":compose:integration-tests:macrobenchmark",
                ":compose:integration-tests:macrobenchmark-target"
            ),
            setOf(
                ":emoji2:integration-tests:init-disabled-macrobenchmark",
                ":emoji2:integration-tests:init-disabled-macrobenchmark-target",
            ),
            setOf(
                ":emoji2:integration-tests:init-enabled-macrobenchmark",
                ":emoji2:integration-tests:init-enabled-macrobenchmark-target",
            ),
        )

        private val IGNORED_PATHS = setOf(
            "docs/",
            "development/"
        )
    }
}
