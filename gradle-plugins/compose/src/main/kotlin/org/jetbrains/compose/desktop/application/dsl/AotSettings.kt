package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.jetbrains.compose.internal.utils.packagedAppJarFilesDir
import java.io.Serializable

/**
 * The configuration of AOT for the native distribution.
 *
 * AOT is a collection of JVM mechanisms that allow to speed up application
 * startup and JIT warmup by creating a "class archive" that helps the JVM
 * load and JIT-compile classes faster.
 */
abstract class AotConfiguration : Serializable {
    /**
     * The AOT mode to use.
     */
    var mode: AotMode = AotMode.None

    /**
     * Whether to print AOT-related messages at application runtime.
     */
    var logging: Boolean = false

    /**
     * Whether to fail running the app if unable to load the class archive.
     */
    var exitAppOnAotFailure: Boolean = false

    /**
     * The AOT-related arguments to pass the JVM when running the app.
     */
    internal val runtimeJvmArgs: List<String> by lazy {
        mode.runtimeJvmArgs(this)
    }
}

/**
 * The concrete mode of use of AOT.
 */
abstract class AotMode(val name: String) : Serializable {

    /**
     * The minimum JDK version for which this mode is supported.
     */
    internal open val minJdkVersion: Int? = null

    /**
     * Whether to generate a `classes.jsa` CDS archive for the JRE classes.
     */
    internal abstract val generateJreClassesArchive: Boolean

    /**
     * Returns whether this mode needs a training run before packaging the app.
     */
    internal abstract val needsTrainingRun: Boolean

    /**
     * The arguments to pass to the JVM when executing the training run.
     *
     * This will only be called if [needsTrainingRun] is `true`.
     */
    internal open fun trainingRunJvmArgs(): List<String> =
        error("AotMode '$this' does not need a training run")

    /**
     * Returns the training run's output file (the class archive), given the root directory of
     * the packaged app.
     */
    internal open fun trainingRunClassArchive(packagedAppRootDir: Directory): RegularFile =
        error("AotMode '$this' does not need a training run")

    /**
     * The arguments to pass to the JVM when running the final app.
     */
    internal abstract fun runtimeJvmArgs(configuration: AotConfiguration): List<String>

    /**
     * Checks whether this mode is compatible with the given JDK major version.
     * Throws an exception if not.
     */
    internal open fun checkJdkCompatibility(jdkMajorVersion: Int, jdkVendor: String) {
        val minMajorJdkVersion = minJdkVersion ?: return
        if (jdkMajorVersion < minMajorJdkVersion) {
            error(
                "AotMode '$this' is not supported on JDK earlier than" +
                        " $minMajorJdkVersion; current is $jdkMajorVersion"
            )
        }
    }

    override fun toString() = name

    /**
     * AOT is not used.
     */
    data object None : AotMode("None") {
        override val needsTrainingRun: Boolean get() = false
        override val generateJreClassesArchive: Boolean get() = false
        override fun runtimeJvmArgs(configuration: AotConfiguration) = emptyList<String>()
        @Suppress("unused") private fun readResolve(): Any = None
    }

    /**
     * The base class for the AppCDS modes introduced in JDK 21.
     */
    abstract class AppCdsMode(name: String) : AotMode(name) {
        companion object {
            /**
             * The name of the AppCDS archive file.
             */
            internal const val ARCHIVE_NAME = "app.jsa"

            /**
             * The AppCDS archive file.
             */
            internal const val ARCHIVE_FILE_ARGUMENT = "\$APPDIR/$ARCHIVE_NAME"
        }

        /**
         * Returns the options that need to be added due to the configuration.
         */
        protected fun MutableList<String>.addConfigOptions(configuration: AotConfiguration) {
            if (configuration.exitAppOnAotFailure) {
                add("-Xshare:on")
            }
            if (configuration.logging) {
                add("-Xlog:cds")
            }
        }
    }

    /**
     * AppCDS is used via a dynamic shared archive created automatically
     * when the app is run (using `-XX:+AutoCreateSharedArchive`).
     *
     * Advantages:
     * - Simplest - no additional step is needed to build the archive.
     * - Creates a smaller distributable.
     *
     * Drawbacks:
     * - Requires JDK 19 or later.
     * - The archive is not available at the first execution of the app,
     *   so it is slower (and possibly even slower than regular execution).
     *   The archive is created when at shutdown time of the first execution,
     *   which also takes a little longer.
     * - Some OSes may block writing the archive file to the application's
     *   directory at runtime. Due to this, `AppCdsAuto` mode is mostly
     *   recommended only to experiment and see how fast your app starts up
     *   with AppCDS.
     *   In production, we recommend [AppCdsPrebuild] mode.
     */
    data object AppCdsAuto : AppCdsMode("AppCdsAuto") {
        override val minJdkVersion = 19
        override val needsTrainingRun: Boolean get() = false
        override val generateJreClassesArchive: Boolean get() = true
        override fun runtimeJvmArgs(configuration: AotConfiguration) = buildList {
            addConfigOptions(configuration)
            add("-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT")
            add("-XX:+AutoCreateSharedArchive")
        }

        @Suppress("unused") private fun readResolve(): Any = AppCdsAuto
    }

    /**
     * Uses the AppCDS feature introduced in JDK 21 where the class archive is
     * created via a training run with the `-XX:ArchiveClassesAtExit` JVM
     * argument.
     *
     * When using this mode, the app will be run during the creation of
     * the distributable. In this run, the system property
     * `compose.aot.training-run` will be set to the value `true`.
     * The app should "exercise" itself and cause the loading of all
     * the classes that should be written into the archive.
     * It should then shut down to let the build process continue.
     *
     * For example:
     * ```
     * application {
     *     ...
     *     if (System.getProperty("compose.aot.training-run") == "true") {
     *         LaunchedEffect(Unit) {
     *             delay(10.seconds)  // Or a custom event indicating startup finished
     *             exitApplication()
     *         }
     *     }
     * }
     * ```
     *
     * Advantages:
     * - Fast startup, including the first run.
     *
     * Drawbacks:
     * - Requires JDK 21 or later.
     * - Requires an additional step (the training run) when building the distributable.
     * - The distributable is larger because it includes the archive of the app's classes.
     */
    data object AppCdsPrebuild : AppCdsMode("AppCdsPrebuild") {
        override val minJdkVersion = 21
        override val generateJreClassesArchive: Boolean get() = true
        override val needsTrainingRun: Boolean get() = true
        override fun trainingRunJvmArgs() =
            listOf(
                "-XX:ArchiveClassesAtExit=$ARCHIVE_FILE_ARGUMENT",
                "-Xlog:cds",
                SET_TRAINING_RUN_SYSTEM_PROPERTY_JVM_ARG
            )
        override fun trainingRunClassArchive(packagedAppRootDir: Directory): RegularFile {
            val appDir = packagedAppJarFilesDir(packagedAppRootDir)
            return appDir.file(ARCHIVE_NAME)
        }
        override fun runtimeJvmArgs(configuration: AotConfiguration) = buildList {
            addConfigOptions(configuration)
            add("-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT")
        }

        @Suppress("unused") private fun readResolve(): Any = AppCdsPrebuild
    }

    /**
     * Uses AOT feature introduced in JDK 25 where the class archive is
     * created via a training run with the `-XX:AOTCacheOutput` JVM
     * argument.
     *
     * This mode is similar to [AppCdsPrebuild], but the class archive
     * created by AOT goes deeper into the class loading pipeline, and
     * includes linking. It also includes extra information to help the JIT.
     *
     * When using this mode, the app will be run during the creation of
     * the distributable. In this run, the system property,
     * `compose.aot.training-run` will be set to the value `true`.
     * The app should "exercise" itself and cause the loading of all
     * the classes that should be written into the archive.
     * It should then shut down to let the build process continue.
     *
     * For example:
     * ```
     * application {
     *     ...
     *     if (System.getProperty("compose.aot.training-run") == "true") {
     *         LaunchedEffect(Unit) {
     *             delay(10.seconds)  // Or a custom event indicating startup finished
     *             exitApplication()
     *         }
     *     }
     * }
     * ```
     *
     * Advantages:
     * - Fast startup, including the first run.
     * - Better JIT warmup.
     *
     * Drawbacks:
     * - Requires JDK 25 or later.
     * - Requires an additional step (the training run) when building the
     *   distributable.
     * - The distributable is larger because it includes the archive of
     *   the app's classes.
     */
    data object AotPrebuild : AotMode("AotPrebuild") {

        internal const val ARCHIVE_NAME = "app.aot"
        internal const val ARCHIVE_FILE_ARGUMENT = "\$APPDIR/$ARCHIVE_NAME"

        override val minJdkVersion = 25
        override val generateJreClassesArchive: Boolean get() = true
        override val needsTrainingRun: Boolean get() = true
        override fun trainingRunJvmArgs() =
            listOf(
                "-XX:AOTCacheOutput=$ARCHIVE_FILE_ARGUMENT",
                "-Xlog:aot",
                SET_TRAINING_RUN_SYSTEM_PROPERTY_JVM_ARG,
            )
        override fun trainingRunClassArchive(packagedAppRootDir: Directory): RegularFile {
            val appDir = packagedAppJarFilesDir(packagedAppRootDir)
            return appDir.file(ARCHIVE_NAME)
        }
        override fun runtimeJvmArgs(configuration: AotConfiguration) = buildList {
            add("-XX:AOTCache=$ARCHIVE_FILE_ARGUMENT")
            if (configuration.exitAppOnAotFailure) {
                add("-XX:AOTMode=on")
            }
            if (configuration.logging) {
                add("-Xlog:aot")
            }
        }

        @Suppress("unused") private fun readResolve(): Any = AotPrebuild
    }

}

private const val SET_TRAINING_RUN_SYSTEM_PROPERTY_JVM_ARG = "-Dcompose.aot.training-run=true"