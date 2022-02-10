/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.jetbrains.compose.desktop.application.internal.files.isDylibPath
import java.io.File
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

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
        // Only resign modify the runtime if a provisioning profile or alternative entitlements file is provided.
        // If no entitlements file is provided, the runtime cannot be resigned.
        if (runtimeProvisioningProfile == null &&
            // When resigning the runtime, an app entitlements file is also needed.
            (runtimeEntitlementsFile == null || entitlementsFile == null)
        ) {
            return
        }

        // Add the provisioning profile
        runtimeProvisioningProfile?.let {
            addRuntimeProvisioningProfile(runtimeDir, it)
        }

        // Resign the runtime completely (and also the app dir only)
        resignRuntimeAndAppDir(appDir, runtimeDir)
    }

    private fun addRuntimeProvisioningProfile(
        runtimeDir: File,
        runtimeProvisioningProfile: File
    ) {
        runtimeProvisioningProfile.copyTo(
            target = runtimeDir.resolve("Contents/embedded.provisionprofile"),
            overwrite = true
        )
    }

    private fun resignRuntimeAndAppDir(
        appDir: File,
        runtimeDir: File
    ) {
        // Sign all libs and executables in runtime
        runtimeDir.walk().forEach { file ->
            val path = file.toPath()
            if (path.isRegularFile() && (path.isExecutable() || path.toString().isDylibPath)) {
                if (path.isSymbolicLink()) {
                    // Ignore symbolic links
                } else {
                    // Resign file
                    macSigner.unsign(file)
                    macSigner.sign(file, runtimeEntitlementsFile)
                }
            }
        }

        // Resign runtime directory
        macSigner.unsign(runtimeDir)
        macSigner.sign(runtimeDir, runtimeEntitlementsFile, forceEntitlements = true)

        // Resign app directory (contents other than runtime were already signed by jpackage)
        macSigner.unsign(appDir)
        macSigner.sign(appDir, entitlementsFile, forceEntitlements = true)
    }
}
