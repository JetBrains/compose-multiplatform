package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.internal.impldep.kotlinx.serialization.SerialInfo
import org.jetbrains.compose.internal.utils.packagedAppJarFilesDir
import java.io.Serializable

/**
 * The configuration of AppCDS for the native distribution.
 *
 * AppCDS is a JVM mechanism that allows to significantly speed up application
 * startup by creating an archive of the classes it uses that can be loaded
 * and used much faster than class files.
 */
abstract class AppCdsConfiguration : Serializable {
    /**
     * The AppCDS mode to use.
     */
    var mode: AppCdsMode = AppCdsMode.None

    /**
     * Whether to print AppCDS-related messages at application runtime.
     */
    var logging: Boolean = false

    /**
     * Whether to fail running the app if unable to load the AppCDS archive.
     */
    var exitAppOnCdsFailure: Boolean = false

    /**
     * Returns the AppCDS-related arguments to pass the JVM when running the app.
     */
    internal val runtimeJvmArgs: List<String> by lazy {
        mode.runtimeJvmArgs(this)
    }
}

/**
 * The mode of use of AppCDS.
 */
abstract class AppCdsMode(val name: String) : Serializable {

    /**
     * The minimum JDK version for which this mode is supported.
     */
    internal open val minJdkVersion: Int? = null

    /**
     * Whether to generate a classes.jsa archive for the JRE classes.
     */
    internal abstract val generateJreClassesArchive: Boolean

    /**
     * Returns whether this mode needs a training run before packaging the app.
     */
    internal open val needsTrainingRun: Boolean get() = false

    /**
     * The arguments to pass to the JVM when executing the training.
     *
     * This will only be called if [needsTrainingRun] is `true`.
     */
    internal open fun trainingRunJvmArgs(): List<String> =
        error("AppCdsMode '$this' does not need a training run")

    /**
     * Returns the training run's output file (the archive of app classes), given the root directory of
     * the packaged app.
     */
    internal open fun trainingRunOutputFile(packagedAppRootDir: Directory): RegularFile =
        error("AppCdsMode '$this' does not need a training run")

    /**
     * The arguments to pass to the JVM when running the final app.
     *
     * When [configuration] is `null`, don't return any configuration-specific arguments.
     */
    internal abstract fun runtimeJvmArgs(configuration: AppCdsConfiguration?): List<String>

    /**
     * Checks whether this mode is compatible with the given JDK major version.
     * Throws an exception if not.
     */
    internal open fun checkJdkCompatibility(jdkMajorVersion: Int, jdkVendor: String) {
        val minMajorJdkVersion = minJdkVersion ?: return
        if (jdkMajorVersion < minMajorJdkVersion) {
            error(
                "AppCdsMode '$this' is not supported on JDK earlier than" +
                        " $minMajorJdkVersion; current is $jdkMajorVersion"
            )
        }
    }

    override fun toString() = name

    /**
     * AppCDS is not used.
     */
    data object None : AppCdsMode("None") {
        override val generateJreClassesArchive: Boolean get() = false
        override fun runtimeJvmArgs(configuration: AppCdsConfiguration?) = emptyList<String>()
        @Suppress("unused") private fun readResolve(): Any = None
    }

    abstract class Jdk21AppCdsMode(name: String) : AppCdsMode(name) {

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

        protected fun MutableList<String>.addConfigOptions(configuration: AppCdsConfiguration?) {
            if (configuration == null) return
            if (configuration.exitAppOnCdsFailure) {
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
     *   so it is slower (and possibly even slower than regular execution),
     *   The archive is created when at shutdown time of the first execution,
     *   which also takes a little longer.
     * - Some OSes may block writing the archive file to the application's
     *   directory at runtime. Due to this, `Auto` mode is mostly recommended
     *   only to experiment and see how fast your app starts up with AppCDS.
     *   In production, we recommend [Prebuild] mode.
     *
     */
    @Suppress("unused")
    data object Auto : Jdk21AppCdsMode("Auto") {
        override val minJdkVersion = 19
        override val generateJreClassesArchive: Boolean get() = true
        override fun runtimeJvmArgs(configuration: AppCdsConfiguration?) = buildList {
            addConfigOptions(configuration)
            add("-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT")
            add("-XX:+AutoCreateSharedArchive")
        }

        @Suppress("unused") private fun readResolve(): Any = Prebuild
    }

    /**
     * AppCDS is used via a dynamic shared archive created by executing
     * the app before packaging (using `-XX:ArchiveClassesAtExit`).
     *
     * When using this mode, the app will be run during the creation of
     * the distributable. In this run, a system property
     * `compose.appcds.create-archive` will be set to the value `true`.
     * The app should "exercise" itself and cause the loading of all
     * the classes that should be written into the AppCDS archive. It
     * should then shut down in order to let the build process continue.
     *
     * For example:
     * ```
     * application {
     *     ...
     *     if (System.getProperty("compose.appcds.create-archive") == "true") {
     *         LaunchedEffect(Unit) {
     *             delay(10.seconds)  // Or a custom event indicating startup finished
     *             exitApplication()
     *         }
     *     }
     * }
     * ```
     *
     * Advantages:
     * - The first run of the distributed app is fast too.
     *
     * Drawbacks:
     * - Requires JDK 21 or later.
     * - Requires an additional step of running the app when building the
     *   distributable.
     * - The distributable is larger because it includes the archive of
     *   the app's classes.
     */
    @Suppress("unused")
    data object Prebuild : Jdk21AppCdsMode("Prebuild") {
        override val minJdkVersion = 21
        override fun checkJdkCompatibility(jdkMajorVersion: Int, jdkVendor: String) {
            super.checkJdkCompatibility(jdkMajorVersion, jdkVendor)
//                if (jdkVendor != "JetBrains s.r.o.") {
//                    error(
//                        "Prebuild AppCDS mode is only supported on JetBrains JDK; " +
//                                "current vendor is '$jdkVendor'"
//                    )
//                }
        }
        override val generateJreClassesArchive: Boolean get() = true
        override val needsTrainingRun: Boolean get() = true
        override fun trainingRunJvmArgs() =
            listOf(
                "-XX:ArchiveClassesAtExit=$ARCHIVE_FILE_ARGUMENT",
                "-Dcompose.appcds.create-archive=true",
            )
        override fun trainingRunOutputFile(packagedAppRootDir: Directory): RegularFile {
            val appDir = packagedAppJarFilesDir(packagedAppRootDir)
            return appDir.file(ARCHIVE_NAME)
        }
        override fun runtimeJvmArgs(configuration: AppCdsConfiguration?) = buildList {
            addConfigOptions(configuration)
            add("-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT")
        }

        @Suppress("unused") private fun readResolve(): Any = Prebuild
    }
}