package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.jetbrains.compose.internal.utils.packagedAppJarFilesDir
import java.io.Serializable

/**
 * The configuration of AppCDS for the native distribution.
 *
 * AppCDS is a JVM mechanism that allows to significantly speed up application
 * startup by creating an archive of the classes it uses that can be loaded
 * and used much faster than class files.
 */
abstract class AppCdsConfiguration {
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
}

/**
 * Returns the AppCDS-related arguments to pass the JVM when running the app.
 */
internal fun AppCdsConfiguration.runtimeJvmArgs() = buildList {
    addAll(mode.runtimeJvmArgs())
    if (exitAppOnCdsFailure && (mode != AppCdsMode.None)) {
        add("-Xshare:on")
    }
    if (logging) {
        add("-Xlog:cds")
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
    internal open fun appClassesArchiveFile(packagedAppRootDir: Directory): RegularFile =
        error("AppCdsMode '$this' does not create an archive")

    /**
     * The arguments to pass to the JVM when running the final app.
     */
    internal abstract fun runtimeJvmArgs(): List<String>

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
        val Auto = object : AppCdsMode("Auto") {
            override val minJdkVersion = 19
            override val generateJreClassesArchive: Boolean get() = true
            override fun runtimeJvmArgs() =
                listOf(
                    "-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT",
                    "-XX:+AutoCreateSharedArchive"
                )
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
        val Prebuild = object : AppCdsMode("Prebuild") {
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
            override val generateAppClassesArchive: Boolean get() = true
            override fun appClassesArchiveCreationJvmArgs() =
                listOf(
                    "-XX:ArchiveClassesAtExit=$ARCHIVE_FILE_ARGUMENT",
                    "-XX:+NoClasspathInArchive",
                    "-Dcompose.appcds.create-archive=true",
                )
            override fun appClassesArchiveFile(packagedAppRootDir: Directory): RegularFile {
                val appDir = packagedAppJarFilesDir(packagedAppRootDir)
                return appDir.file(ARCHIVE_NAME)
            }
            override fun runtimeJvmArgs() =
                listOf(
                    "-XX:SharedArchiveFile=$ARCHIVE_FILE_ARGUMENT",
                    "-XX:+NoClasspathInArchive",
                )
        }
    }
}