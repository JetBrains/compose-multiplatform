package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.logging.Logger
import org.jetbrains.compose.internal.utils.MacUtils
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

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
            val dbFactory = DocumentBuilderFactory.newInstance()
            // Disable DTD loading to prevent XXE vulnerabilities and issues with network access or missing DTDs
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            val dBuilder = dbFactory.newDocumentBuilder()
            val xmlInput = org.xml.sax.InputSource(StringReader(outputContent))
            val doc = dBuilder.parse(xmlInput)
            doc.documentElement.normalize() // Recommended practice
            val nodeList = doc.getElementsByTagName("key")
            var version: String? = null
            for (i in 0 until nodeList.length) {
                if ("short-bundle-version" == nodeList.item(i).textContent) {
                    // Find the next sibling element which should be <string>
                    var nextSibling = nodeList.item(i).nextSibling
                    while (nextSibling != null && nextSibling.nodeType != org.w3c.dom.Node.ELEMENT_NODE) {
                        nextSibling = nextSibling.nextSibling
                    }
                    if (nextSibling != null && nextSibling.nodeName == "string") {
                        version = nextSibling.textContent
                        break
                    }
                }
            }
            version
        } catch (e: Exception) {
            error("Could not parse actool version XML from output: '$outputContent'. Error: ${e.message}")
        }

        if (versionString == null) {
            error("Could not extract short-bundle-version from actool output: '$outputContent'. Assuming it meets requirements.")
        }

        val majorVersion = versionString.split(".").firstOrNull()?.toIntOrNull()
        if (majorVersion == null) {
            error("Could not get actool major version from version string '$versionString' . Output was: '$outputContent'. Assuming it meets requirements.")
        }

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