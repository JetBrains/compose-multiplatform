package org.jetbrains.compose.desktop.application.dsl

import org.jetbrains.compose.internal.utils.packagedAppJarFilesDir
import java.io.File
import java.io.Serializable

/**
 * The configuration of AppCDS for the native distribution.
 */
abstract class AppCdsConfiguration {
    /**
     * The AppCDS mode to use.
     */
    var mode: AppCdsMode = AppCdsMode.None
}

/**
 * Returns the AppCDS-related arguments to pass the JVM when running the app.
 */
internal fun AppCdsConfiguration.runtimeJvmArgs() = mode.runtimeJvmArgs()

/**
 * The mode of use of AppCDS.
 */
abstract class AppCdsMode(val name: String) : Serializable {

    /**
     * Whether to generate a classes.jsa archive for the JRE classes.
     */
    internal abstract val generateJreClassesArchive: Boolean

    /**
     * Returns whether this mode creates an archive of app classes at build time.
     */
    internal open val generateAppClassesArchive: Boolean get() = false

    /**
     * The arguments to pass to the JVM when running the app to create
     * the archive for the app's class files.
     *
     * This will only be called if [generateAppClassesArchive] is `true`.
     */
    internal open fun appClassesArchiveCreationJvmArgs(): List<String> =
        error("AppCdsMode '$this' does not create an archive")

    /**
     * Returns the app's classes archive file, given the root directory of
     * the packaged app.
     */
    internal open fun appClassesArchiveFile(packagedAppRootDir: File): File =
        error("AppCdsMode '$this' does not create an archive")

    /**
     * The arguments to pass to the JVM when running the final app.
     */
    internal abstract fun runtimeJvmArgs(): List<String>

    /**
     * Checks whether this mode is compatible with the given JDK major version.
     * Throws an exception if not.
     */
    internal open fun checkJdkCompatibility(jdkMajorVersion: Int) = Unit

    override fun toString() = name

    companion object {

        /**
         * The name of the AppCDS archive file.
         */
        private const val ARCHIVE_NAME = "app.jsa"

        /**
         * The AppCDS archive file.
         */
        internal const val ARCHIVE_FILE_ARGUMENT = "\$APPDIR/$ARCHIVE_NAME"

        /**
         * AppCDS is not used.
         */
        val None = object : AppCdsMode("None") {
            override val generateJreClassesArchive: Boolean get() = false
            override fun runtimeJvmArgs() = emptyList<String>()
        }

        /**
         * AppCDS is used via a dynamic shared archive created automatically
         * when the app is run (using `-XX:+AutoCreateSharedArchive`).
         * Due to the drawbacks below, this mode is mostly recommended only
         * to experiment and see how fast your app starts up with AppCDS.
         * In production, we recomment [Prebuild] mode.
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
         * - Some OSes may block the creation of the archive file at runtime.
         */
        @Suppress("unused")
        val Auto = object : AppCdsMode("Auto") {
            private val MIN_JDK_VERSION = 19
            override val generateJreClassesArchive: Boolean get() = true
            override fun runtimeJvmArgs() =
                listOf(
                    "-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT",
                    "-XX:+AutoCreateSharedArchive"
                )
            override fun checkJdkCompatibility(jdkMajorVersion: Int) {
                if (jdkMajorVersion < MIN_JDK_VERSION) {
                    error(
                        "AppCdsMode '$this' is not supported on JDK earlier than" +
                                " $MIN_JDK_VERSION; current is $jdkMajorVersion"
                    )
                }
            }
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
         * Advantages:
         * - Can be used with JDKs earlier than 19.
         * - The first run of the distributed app is fast too.
         *
         * Drawbacks:
         * - Requires an additional step of running the app when building the
         *   distributable.
         * - The distributable is larger because it includes the archive of
         *   the app's classes.
         */
        @Suppress("unused")
        val Prebuild = object : AppCdsMode("Prebuild") {
            override val generateJreClassesArchive: Boolean get() = true
            override val generateAppClassesArchive: Boolean get() = true
            override fun appClassesArchiveCreationJvmArgs() =
                listOf(
                    "-XX:ArchiveClassesAtExit=$ARCHIVE_FILE_ARGUMENT",
                    "-Dcompose.appcds.create-archive=true"
                )
            override fun appClassesArchiveFile(packagedAppRootDir: File): File {
                val appDir = packagedAppJarFilesDir(packagedAppRootDir)
                return appDir.resolve(ARCHIVE_NAME)
            }
            override fun runtimeJvmArgs() =
                listOf(
                    "-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT",
                )
        }
    }
}