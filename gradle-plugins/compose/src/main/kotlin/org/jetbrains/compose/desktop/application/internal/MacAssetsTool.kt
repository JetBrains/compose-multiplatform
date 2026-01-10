package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.logging.Logger
import org.jetbrains.compose.internal.utils.MacUtils
import java.io.File

internal class MacAssetsTool(private val runTool: ExternalToolRunner, private val logger: Logger) {

    fun compileAssets(iconDir: File, workingDir: File, minimumSystemVersion: String?): File {
        val toolVersion = checkAssetsToolVersion()
        logger.info("compile mac assets is starting, supported actool version:$toolVersion")

        val result = runTool(
            tool = MacUtils.xcrun,
            args = listOf(
                "actool",
                iconDir.absolutePath, // Input asset catalog
                "--compile", workingDir.absolutePath,
                "--app-icon", iconDir.name.removeSuffix(".icon"),
                "--enable-on-demand-resources", "NO",
                "--development-region", "en",
                "--target-device", "mac",
                "--platform", "macosx",
                "--enable-icon-stack-fallback-generation=disabled",
                "--include-all-app-icons",
                "--minimum-deployment-target", minimumSystemVersion ?: "10.13",
                "--output-partial-info-plist", "/dev/null"
            ),
        )

        if (result.exitValue != 0) {
            error("Could not compile the layered icons directory into Assets.car.")
        }
        if (!assetsFile(workingDir).exists()) {
            error("Could not find Assets.car in the working directory.")
        }
        return workingDir.resolve("Assets.car")
    }

    fun assetsFile(workingDir: File): File = workingDir.resolve("Assets.car")

    private fun checkAssetsToolVersion(): String {
        val requiredVersion = 26.0
        var outputContent = ""
        val result = runTool(
            tool = MacUtils.xcrun,
            args = listOf("actool", "--version"),
            processStdout = { outputContent = it },
        )

        if (result.exitValue != 0) {
            error("Could not get actool version: Command `xcrun actool -version` exited with code ${result.exitValue}\nStdOut: $outputContent\n")
        }

        val versionString: String? = try {
            var versionContent = ""
            runTool(
                tool = MacUtils.plutil,
                args = listOf(
                    "-extract",
                    "com\\.apple\\.actool\\.version.short-bundle-version",
                    "raw",
                    "-expect",
                    "string",
                    "-o",
                    "-",
                    "-"
                ),
                stdinStr = outputContent,
                processStdout = {
                    versionContent = it
                }
            )
            versionContent
        } catch (e: Exception) {
            error("Could not check actool version. Error: ${e.message}")
        }

        if (versionString.isNullOrBlank()) {
            error("Could not extract short-bundle-version from actool output: '$outputContent'. Assuming it meets requirements.")
        }

        val majorVersion = versionString
            .split(".")
            .firstOrNull()
            ?.toIntOrNull()
            ?: error("Could not get actool major version from version string '$versionString' . Output was: '$outputContent'. Assuming it meets requirements.")

        if (majorVersion < requiredVersion) {
            error(
                "Unsupported actool version: $versionString. " +
                        "Version $requiredVersion or higher is required. " +
                        "Please update your Xcode Command Line Tools."
            )
        } else {
            return versionString
        }
    }
}
