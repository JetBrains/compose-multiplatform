/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import java.util.*
import javax.inject.Inject

abstract class AbstractDistributions {
    @get:Inject
    internal abstract val objects: ObjectFactory

    @get:Inject
    internal abstract val layout: ProjectLayout

    val outputBaseDir: DirectoryProperty = objects.directoryProperty().apply {
        set(layout.buildDirectory.dir("compose/binaries"))
    }

    var packageName: String? = null
    var packageVersion: String? = null
    var copyright: String? = null

    var description: String? = null
    var vendor: String? = null
    val appResourcesRootDir: DirectoryProperty = objects.directoryProperty()

    /**
     * Root directory for files passed to the underlying packaging tool (jpackage)
     * via `--resource-dir`, used to override the tool's own packaging resources (e.g.
     * a `<package-name>-volume.icns` DMG volume icon, a DMG background, Linux
     * `preinst`/`postinst` scripts, or WiX templates). See
     * https://docs.oracle.com/en/java/javase/21/jpackage/override-jpackage-resources.html
     *
     * Files are resolved from `common`, `<os>` and `<os>-<arch>` subdirectories, the
     * same layout as [appResourcesRootDir]. The generated `Info.plist` and
     * `product-def.plist` are managed by Compose and always take precedence.
     */
    val packagingResourcesRootDir: DirectoryProperty = objects.directoryProperty()

    val licenseFile: RegularFileProperty = objects.fileProperty()

    var targetFormats: Set<TargetFormat> = EnumSet.noneOf(TargetFormat::class.java)
    open fun targetFormats(vararg formats: TargetFormat) {
        targetFormats = EnumSet.copyOf(formats.toList())
    }
}