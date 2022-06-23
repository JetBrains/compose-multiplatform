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
import androidx.build.gradle.isRoot
import java.io.File
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec

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
 * Provides the list of file paths (relative to the git root) that have changed (can include
 * removed files).
 *
 * Returns `null` if changed files cannot be detected.
 */
typealias ChangedFilesProvider = () -> List<String>?

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
    abstract fun shouldInclude(project: String): Boolean

    /**
     * Returns whether this task was affected by current changes.
     */
    open fun shouldInclude(task: Task): Boolean {
        val include = shouldInclude(task.project.path)
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
    abstract fun getSubset(projectPath: String): ProjectSubset

    fun getSubset(task: Task): ProjectSubset {
        val taskPath = task.path
        val lastColonIndex = taskPath.lastIndexOf(":")
        val projectPath = taskPath.substring(0, lastColonIndex)
        return getSubset(projectPath)
    }

    companion object {
        private const val ROOT_PROP_NAME = "affectedModuleDetector"
        private const val SERVICE_NAME = ROOT_PROP_NAME + "BuildService"
        private const val LOG_FILE_NAME = "affected_module_detector_log.txt"
        const val ENABLE_ARG = "androidx.enableAffectedModuleDetection"
        const val BASE_COMMIT_ARG = "androidx.affectedModuleDetector.baseCommit"

        @JvmStatic
        fun configure(gradle: Gradle, rootProject: Project) {
            // Make an AffectedModuleDetectorWrapper that callers can save before the real
            // AffectedModuleDetector is ready. Callers won't be able to use it until the wrapped
            // detector has been assigned, but configureTaskGuard can still reference it in
            // closures that will execute during task execution.
            val instance = AffectedModuleDetectorWrapper()
            rootProject.extensions.add(ROOT_PROP_NAME, instance)

            val enabled = rootProject.hasProperty(ENABLE_ARG) &&
                rootProject.findProperty(ENABLE_ARG) != "false"

            val distDir = rootProject.getDistributionDirectory()
            val outputFile = distDir.resolve(LOG_FILE_NAME)

            outputFile.writeText("")
            val logger = FileLogger(outputFile)
            logger.info("setup: enabled: $enabled")
            if (!enabled) {
                val provider = setupWithParams(
                    rootProject,
                    { spec ->
                        val params = spec.parameters
                        params.acceptAll = true
                        params.log = logger
                    }
                )
                logger.info("using AcceptAll")
                instance.wrapped = provider
                return
            }
            val baseCommitOverride: String? = rootProject.findProperty(BASE_COMMIT_ARG) as String?
            if (baseCommitOverride != null) {
                logger.info("using base commit override $baseCommitOverride")
            }
            val changeInfoPath = GitClient.getChangeInfoPath(rootProject)
            val manifestPath = GitClient.getManifestPath(rootProject)
            gradle.taskGraph.whenReady {
                logger.lifecycle("projects evaluated")
                val projectGraph = ProjectGraph(rootProject)
                val dependencyTracker = DependencyTracker(rootProject, logger.toLogger())
                val provider = setupWithParams(
                    rootProject,
                    { spec ->
                        val params = spec.parameters
                        params.rootDir = rootProject.projectDir
                        params.projectGraph = projectGraph
                        params.dependencyTracker = dependencyTracker
                        params.log = logger
                        params.baseCommitOverride = baseCommitOverride
                        params.changeInfoPath = changeInfoPath
                        params.manifestPath = manifestPath
                    }
                )
                logger.info("using real detector")
                instance.wrapped = provider
            }
        }

        private fun setupWithParams(
            rootProject: Project,
            configureAction: Action<BuildServiceSpec<AffectedModuleDetectorLoader.Parameters>>
        ): Provider<AffectedModuleDetectorLoader> {
            if (!rootProject.isRoot) {
                throw IllegalArgumentException("this should've been the root project")
            }
            return rootProject.gradle.sharedServices
                .registerIfAbsent(
                    SERVICE_NAME,
                    AffectedModuleDetectorLoader::class.java,
                    configureAction
                )
        }

        fun getInstance(project: Project): AffectedModuleDetector {
            val extensions = project.rootProject.extensions
            @Suppress("UNCHECKED_CAST")
            val detector = extensions.findByName(ROOT_PROP_NAME) as? AffectedModuleDetector
            return detector!!
        }

        /**
         * Call this method to configure the given task to execute only if the owner project
         * is affected by current changes
         */
        @Throws(GradleException::class)
        @JvmStatic
        fun configureTaskGuard(task: Task) {
            val detector = getInstance(task.project)
            task.onlyIf {
                detector.shouldInclude(task)
            }
        }
    }
}

/**
 * Wrapper for AffectedModuleDetector
 * Callers can access this wrapper during project configuration and save it until task execution
 * time when the wrapped detector is ready for use (after the project graph is ready)
 */
class AffectedModuleDetectorWrapper : AffectedModuleDetector(logger = null) {
    // We save a provider to a build service that knows how to make an
    // AffectedModuleDetectorImpl because:
    // An AffectedModuleDetectorImpl saves the list of modified files and affected
    // modules to avoid having to recompute it for each task. However, that list can
    // change across builds and we want to recompute it in each build. This requires
    // creating a new AffectedModuleDetectorImpl in each build.
    // To get Gradle to create a new AffectedModuleDetectorImpl in each build, we need
    // to pass around a provider to a build service and query it from each task.
    // The build service gets recreated when absent and reused when present. Then the
    // build service will return the same AffectedModuleDetectorImpl for each task in
    // a build
    var wrapped: Provider<AffectedModuleDetectorLoader>? = null

    fun getOrThrow(): AffectedModuleDetector {
        return wrapped?.get()?.detector ?: throw GradleException(
            """
                        Tried to get the affected module detector implementation too early.
                        You cannot access it until all projects are evaluated.
            """.trimIndent()
        )
    }
    override fun getSubset(projectPath: String): ProjectSubset {
        return getOrThrow().getSubset(projectPath)
    }
    override fun shouldInclude(project: String): Boolean {
        return getOrThrow().shouldInclude(project)
    }
    override fun shouldInclude(task: Task): Boolean {
        return getOrThrow().shouldInclude(task)
    }
}

/**
 * Stores the parameters of an AffectedModuleDetector and creates one when needed.
 * The parameters here may be deserialized and loaded from Gradle's configuration cache when the
 * configuration cache is enabled.
 */
abstract class AffectedModuleDetectorLoader :
    BuildService<AffectedModuleDetectorLoader.Parameters> {
    interface Parameters : BuildServiceParameters {
        var acceptAll: Boolean

        var rootDir: File
        var projectGraph: ProjectGraph
        var dependencyTracker: DependencyTracker
        var log: FileLogger?
        var cobuiltTestPaths: Set<Set<String>>?
        var alwaysBuildIfExists: Set<String>?
        var ignoredPaths: Set<String>?
        var baseCommitOverride: String?
        var changeInfoPath: Provider<String>
        var manifestPath: Provider<String>
    }

    val detector: AffectedModuleDetector by lazy {
        val logger = parameters.log!!
        if (parameters.acceptAll) {
            AcceptAll(null)
        } else {
            val baseCommitOverride = parameters.baseCommitOverride
            if (baseCommitOverride != null) {
                logger.info("using base commit override $baseCommitOverride")
            }
            val gitClient = GitClient.create(
                rootProjectDir = parameters.rootDir,
                logger = logger.toLogger(),
                changeInfoPath = parameters.changeInfoPath.get(),
                manifestPath = parameters.manifestPath.get()
            )
            val changedFilesProvider: ChangedFilesProvider = {
                val baseSha = baseCommitOverride ?: gitClient.findPreviousSubmittedChange()
                check(baseSha != null) {
                    "gitClient returned null from findPreviousSubmittedChange"
                }
                val changedFiles = gitClient.findChangedFilesSince(baseSha)
                logger.info("changed files: $changedFiles")
                changedFiles
            }

            AffectedModuleDetectorImpl(
                projectGraph = parameters.projectGraph,
                dependencyTracker = parameters.dependencyTracker,
                logger = logger.toLogger(),
                cobuiltTestPaths = parameters.cobuiltTestPaths
                    ?: AffectedModuleDetectorImpl.COBUILT_TEST_PATHS,
                alwaysBuildIfExists = parameters.alwaysBuildIfExists
                    ?: AffectedModuleDetectorImpl.ALWAYS_BUILD_IF_EXISTS,
                ignoredPaths = parameters.ignoredPaths ?: AffectedModuleDetectorImpl.IGNORED_PATHS,
                changedFilesProvider = changedFilesProvider
            )
        }
    }
}

/**
 * Implementation that accepts everything without checking.
 */
private class AcceptAll(
    logger: Logger? = null
) : AffectedModuleDetector(logger) {
    override fun shouldInclude(project: String): Boolean {
        logger?.info("[AcceptAll] acceptAll.shouldInclude returning true")
        return true
    }

    override fun getSubset(projectPath: String): ProjectSubset {
        logger?.info("[AcceptAll] AcceptAll.getSubset returning CHANGED_PROJECTS")
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
    private val projectGraph: ProjectGraph,
    private val dependencyTracker: DependencyTracker,
    logger: Logger?,
    // used for debugging purposes when we want to ignore non module files
    @Suppress("unused")
    private val ignoreUnknownProjects: Boolean = false,
    private val cobuiltTestPaths: Set<Set<String>> = COBUILT_TEST_PATHS,
    private val alwaysBuildIfExists: Set<String> = ALWAYS_BUILD_IF_EXISTS,
    private val ignoredPaths: Set<String> = IGNORED_PATHS,
    private val changedFilesProvider: ChangedFilesProvider
) : AffectedModuleDetector(logger) {

    private val allProjects by lazy {
        projectGraph.allProjects
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

    val alwaysBuild by lazy {
        alwaysBuildIfExists.filter({ path -> allProjects.contains(path) })
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

    private val buildContainsNonProjectFileChanges by lazy {
        unknownFiles.isNotEmpty()
    }

    override fun shouldInclude(project: String): Boolean {
        return if (project == ":" || buildAll) {
            true
        } else {
            affectedProjects.contains(project)
        }
    }

    override fun getSubset(projectPath: String): ProjectSubset {
        return when {
            changedProjects.contains(projectPath) -> {
                ProjectSubset.CHANGED_PROJECTS
            }
            dependentProjects.contains(projectPath) -> {
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
    private fun findChangedProjects(): Set<String> {
        val changedFiles = changedFilesProvider() ?: return allProjects

        val changedProjects: MutableSet<String> = alwaysBuild.toMutableSet()

        for (filePath in changedFiles) {
            if (ignoredPaths.any { filePath.startsWith(it) }) {
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
    private fun findDependentProjects(): Set<String> {
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

    private fun lookupProjectSetsFromPaths(allSets: Set<Set<String>>): Set<Set<String>> {
        return allSets.map { setPaths ->
            var setExists = false
            val projectSet = HashSet<String>()
            for (path in setPaths) {
                if (!allProjects.contains(path)) {
                    if (setExists) {
                        throw IllegalStateException(
                            "One of the projects in the group of projects that are required to " +
                                "be built together is missing. Looked for " + setPaths
                        )
                    }
                } else {
                    setExists = true
                    projectSet.add(path)
                }
            }
            return@map projectSet
        }.toSet()
    }

    private fun getAffectedCobuiltProjects(
        affectedProjects: Set<String>,
        allCobuiltSets: Set<Set<String>>
    ): Set<String> {
        val cobuilts = mutableSetOf<String>()
        affectedProjects.forEach { project ->
            allCobuiltSets.forEach { cobuiltSet ->
                if (cobuiltSet.any { project == it }) {
                    cobuilts.addAll(cobuiltSet)
                }
            }
        }
        return cobuilts
    }

    private fun findContainingProject(filePath: String): String? {
        return projectGraph.findContainingProject(filePath, logger).also {
            logger?.info("search result for $filePath resulted in $it")
        }
    }

    companion object {
        // Project paths that we always build if they exist
        val ALWAYS_BUILD_IF_EXISTS = setOf(
            // placeholder test project to ensure no failure due to no instrumentation.
            // We can eventually remove if we resolve b/127819369
            ":placeholder-tests",
            ":buildSrc-tests:project-subsets"
        )

        // Some tests are codependent even if their modules are not. Enable manual bundling of tests
        val COBUILT_TEST_PATHS = setOf(
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
            setOf(
                ":wear:benchmark:integration-tests:macrobenchmark",
                ":wear:benchmark:integration-tests:macrobenchmark-target"
            ),
            setOf(
                ":wear:compose:integration-tests:macrobenchmark",
                ":wear:compose:integration-tests:macrobenchmark-target"
            ),
            // Changing generator code changes the output for generated icons, which are tested in
            // material-icons-extended.
            setOf(
                ":compose:material:material:icons:generator",
                ":compose:material:material-icons-extended"
            ),
        )

        val IGNORED_PATHS = setOf(
            "docs/",
            "development/",
            "playground-common/",
            ".github/",
        )
    }
}
