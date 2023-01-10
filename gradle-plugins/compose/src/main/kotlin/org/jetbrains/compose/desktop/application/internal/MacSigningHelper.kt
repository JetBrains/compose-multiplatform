/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.files.isDylibPath
import java.io.File
import java.nio.file.*
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

internal class MacSigningHelper(
    private val macSigner: MacSigner,
    private val runtimeProvisioningProfile: File?,
    private val entitlementsFile: File?,
    private val runtimeEntitlementsFile: File?,
    destinationDir: File,
    packageName: String
) {
    private val appDir = destinationDir.resolve("$packageName.app")
    private val runtimeDir = appDir.resolve("Contents/runtime")

    fun modifyRuntimeIfNeeded() {


        // Resign the runtime completely (and also the app dir only)
        // Sign all libs and executables in runtime
        runtimeDir.walk().forEach { file ->
            val path = file.toPath()
            if (path.isRegularFile(LinkOption.NOFOLLOW_LINKS) && (path.isExecutable() || file.name.isDylibPath)) {
                macSigner.sign(file, runtimeEntitlementsFile)
            }
        }

        macSigner.sign(runtimeDir, runtimeEntitlementsFile, forceEntitlements = true)
        macSigner.sign(appDir, entitlementsFile, forceEntitlements = true)
    }
}
