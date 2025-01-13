/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        val frameworkName = "CMPTestUtils"
        val buildSchemeName = frameworkName
        val objcDir = File(project.projectDir, "src/iosMain/objc")
        val frameworkSourcesDir = objcDir
        val sdkName: String
        val destination: String
        val architecture: String
        if (iosTarget is KotlinNativeTargetWithSimulatorTests) {
            sdkName = "iphonesimulator"
            destination = "generic/platform=iOS Simulator"
            architecture = if (iosTarget.name == "iosSimulatorArm64") "arm64" else "x86_64"
        } else {
            sdkName = "iphoneos"
            destination = "generic/platform=iOS"
            architecture = "arm64"
        }


        val buildDir = project.layout.buildDirectory.dir("objc/${sdkName}.xcarchive").get().asFile.absolutePath
        val frameworkPath = File(buildDir,"/Products/usr/local/lib/lib${frameworkName}.a")
        val headersPath = File(frameworkSourcesDir, frameworkName)

        val compilerArgs = listOf(
            "-include-binary", frameworkPath.absolutePath.toString(),
        ) + "-tr"

        iosTarget.compilations.configureEach {
            val libTaskName = "${compileTaskProvider.name}ObjCLib"
            project.tasks.register(libTaskName, Exec::class.java) {
                inputs.dir(frameworkSourcesDir)
                    .withPropertyName("${frameworkName}-${sdkName}")
                    .withPathSensitivity(PathSensitivity.RELATIVE)

                outputs.cacheIf { true }
                outputs.dir(buildDir)
                    .withPropertyName("${frameworkName}-${sdkName}-archive")

                workingDir(frameworkSourcesDir)
                commandLine("xcodebuild")
                args(
                    "archive",
                    "-scheme", buildSchemeName,
                    "-archivePath", buildDir,
                    "-sdk", sdkName,
                    "-destination", destination,
                    "SKIP_INSTALL=NO",
                    "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
                    "VALID_ARCHS=${architecture}",
                    "MACH_O_TYPE=staticlib"
                )
            }

            tasks[compileTaskProvider.name].dependsOn(libTaskName)

            cinterops.register("test") {
                val cinteropTask = tasks[interopProcessingTaskName]

                headersPath.listFiles()?.forEach {
                    if (it.name.endsWith(".h")) {
                        extraOpts("-header", it.name)
                        compilerOpts("-I${headersPath}")
                    }
                    cinteropTask.inputs.file(it)
                }
            }
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(compilerArgs)
                }
            }
        }
        iosTarget.binaries {
            framework {
                baseName = "InstrumentedTests"
                isStatic = true
                linkerOpts(
                    "-ObjC",
                    "-framework", "UIKit",
                    "-framework", "IOKit"
                )
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(project(":ui-xctest"))
        }
    }
}
