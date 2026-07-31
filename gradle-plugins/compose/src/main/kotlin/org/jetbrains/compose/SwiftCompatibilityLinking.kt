/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.Project
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/** Configures iOS linker tasks to resolve the active Xcode's Swift compatibility-library path at link time. */
internal fun Project.configureSwiftCompatibilityLinking() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.withType(KotlinNativeTarget::class.java).all { target ->
            target.configureSwiftCompatibilityLinking()
        }
    }
}

private fun KotlinNativeTarget.configureSwiftCompatibilityLinking() {
    if (System.getProperty("os.name") != "Mac OS X") return

    val sdkName =
        when (konanTarget) {
            KonanTarget.IOS_ARM64 -> "iphoneos"
            KonanTarget.IOS_X64,
            KonanTarget.IOS_SIMULATOR_ARM64 -> "iphonesimulator"
            else -> return
        }
    val swiftCompatibilityLibraryDir =
        project.providers
            .exec { spec -> spec.commandLine("xcrun", "--find", "swiftc") }
            .standardOutput
            .asText
            .map { swiftcPath ->
                File(swiftcPath.trim())
                    .parentFile
                    .parentFile
                    .parentFile
                    .resolve("usr/lib/swift/$sdkName")
                    .absolutePath
            }

    binaries.withType(NativeBinary::class.java).all { binary ->
        binary.linkTaskProvider.configure { linkTask ->
            linkTask.toolOptions.freeCompilerArgs.addAll(
                swiftCompatibilityLibraryDir.map { libraryDir ->
                    listOf("-linker-option", "-L$libraryDir")
                }
            )
        }
    }
}
