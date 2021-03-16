package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import java.util.*
import javax.inject.Inject

internal val DEFAULT_RUNTIME_MODULES = arrayOf(
    "java.base", "java.desktop", "java.logging"
)

open class NativeDistributions @Inject constructor(
        objects: ObjectFactory,
        layout: ProjectLayout
) {
    var packageName: String? = null
    var description: String? = null
    var copyright: String? = null
    var vendor: String? = null
    @Deprecated(
        "version is deprecated, use packageVersion instead",
        replaceWith = ReplaceWith("packageVersion")
    )
    var version: String?
        get() = packageVersion
        set(value) {
            packageVersion = value
        }
    var packageVersion: String? = null

    val outputBaseDir: DirectoryProperty = objects.directoryProperty().apply {
        set(layout.buildDirectory.dir("compose/binaries"))
    }

    var modules = arrayListOf(*DEFAULT_RUNTIME_MODULES)
    fun modules(vararg modules: String) {
        this.modules.addAll(modules.toList())
    }
    var includeAllModules: Boolean = false

    var targetFormats: Set<TargetFormat> = EnumSet.noneOf(TargetFormat::class.java)
    fun targetFormats(vararg formats: TargetFormat) {
        targetFormats = EnumSet.copyOf(formats.toList())
    }

    val linux: LinuxPlatformSettings = objects.newInstance(LinuxPlatformSettings::class.java)
    fun linux(fn: Action<LinuxPlatformSettings>) {
        fn.execute(linux)
    }

    val macOS: MacOSPlatformSettings = objects.newInstance(MacOSPlatformSettings::class.java)
    fun macOS(fn: Action<MacOSPlatformSettings>) {
        fn.execute(macOS)
    }

    val windows: WindowsPlatformSettings = objects.newInstance(WindowsPlatformSettings::class.java)
    fun windows(fn: Action<WindowsPlatformSettings>) {
        fn.execute(windows)
    }
}