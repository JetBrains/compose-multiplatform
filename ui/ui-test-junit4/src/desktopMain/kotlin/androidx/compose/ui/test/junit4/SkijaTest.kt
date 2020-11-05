/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.initCompose
import org.jetbrains.skija.Surface
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.security.MessageDigest
import java.util.LinkedList

// TODO: replace with androidx.test.screenshot.proto.ScreenshotResultProto after MPP
data class ScreenshotResultProto(
    val result: Status,
    val comparisonStatistics: String,
    val repoRootPath: String,
    val locationOfGoldenInRepo: String,
    val currentScreenshotFileName: String,
    val diffImageFileName: String?,
    val expectedImageFileName: String
) {
    enum class Status {
        UNSPECIFIED,
        PASSED,
        FAILED,
        MISSING_GOLDEN,
        SIZE_MISMATCH
    }
}

data class GoldenConfig(
    val fsGoldenPath: String,
    val repoGoldenPath: String,
    val modulePrefix: String
)

class SkijaTestAlbum(val config: GoldenConfig) {
    data class Report(val screenshots: Map<String, ScreenshotResultProto>)

    private val screenshots: MutableMap<String, ScreenshotResultProto> = mutableMapOf()
    private val report = Report(screenshots)
    fun snap(surface: Surface, id: String) {
        if (!id.matches("^[A-Za-z0-9_-]+$".toRegex())) {
            throw IllegalArgumentException(
                "The given golden identifier '$id' does not satisfy the naming " +
                    "requirement. Allowed characters are: '[A-Za-z0-9_-]'"
            )
        }

        val actual = surface.makeImageSnapshot().encodeToData()!!.bytes

        val expected = readExpectedImage(id)
        if (expected == null) {
            reportResult(
                status = ScreenshotResultProto.Status.MISSING_GOLDEN,
                id = id,
                actual = actual
            )
            return
        }

        val status = if (compareImages(actual = actual, expected = expected)) {
            ScreenshotResultProto.Status.PASSED
        } else {
            ScreenshotResultProto.Status.FAILED
        }

        reportResult(
            status = status,
            id = id,
            actual = actual
        )
    }

    fun check(): Report {
        return report
    }

    private fun dumpImage(path: String, data: ByteArray) {
        val file = File(config.fsGoldenPath, path)
        file.writeBytes(data)
    }

    private val imageExtension = ".png"

    private fun inModuleImagePath(id: String, suffix: String? = null) =
        if (suffix == null) {
            "${config.modulePrefix}/$id$imageExtension"
        } else {
            "${config.modulePrefix}/${id}_$suffix$imageExtension"
        }

    private fun readExpectedImage(id: String): ByteArray? {
        val file = File(config.fsGoldenPath, inModuleImagePath(id))
        if (!file.exists()) {
            return null
        }
        return file.inputStream().readBytes()
    }

    private fun calcHash(input: ByteArray): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(input)
    }

    // TODO: switch to androidx.test.screenshot.matchers.BitmapMatcher#compareBitmaps
    private fun compareImages(actual: ByteArray, expected: ByteArray): Boolean {
        return calcHash(actual).contentEquals(calcHash(expected))
    }

    private fun ensureDir() {
        File(config.fsGoldenPath, config.modulePrefix).mkdirs()
    }

    private fun reportResult(
        status: ScreenshotResultProto.Status,
        id: String,
        actual: ByteArray,
        comparisonStatistics: String? = null
    ) {

        val currentScreenshotFileName: String
        if (status != ScreenshotResultProto.Status.PASSED) {
            currentScreenshotFileName = inModuleImagePath(id, "actual")
            ensureDir()
            dumpImage(currentScreenshotFileName, actual)
        } else {
            currentScreenshotFileName = inModuleImagePath(id)
        }

        screenshots[id] = ScreenshotResultProto(
            result = status,
            comparisonStatistics = comparisonStatistics.orEmpty(),
            repoRootPath = config.repoGoldenPath,
            locationOfGoldenInRepo = inModuleImagePath(id),
            currentScreenshotFileName = currentScreenshotFileName,
            expectedImageFileName = inModuleImagePath(id),
            diffImageFileName = null
        )
    }
}

fun DesktopScreenshotTestRule(
    modulePath: String,
    fsGoldenPath: String = System.getProperty("GOLDEN_PATH"),
    repoGoldenPath: String = "platform/frameworks/support-golden"
): ScreenshotTestRule {
    return ScreenshotTestRule(GoldenConfig(fsGoldenPath, repoGoldenPath, modulePath))
}

class ScreenshotTestRule internal constructor(val config: GoldenConfig) : TestRule {
    private lateinit var testIdentifier: String
    private lateinit var album: SkijaTestAlbum

    companion object {
        init {
            initCompose()
        }
    }

    val executionQueue = LinkedList<() -> Unit>()

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                album = SkijaTestAlbum(config)
                testIdentifier = "${description!!.className}_${description.methodName}"
                    .replace(".", "_").replace(",", "_").replace(" ", "_").replace("__", "_")
                base.evaluate()
                runExecutionQueue()
                handleReport(album.check())
            }
        }
    }

    private fun runExecutionQueue() {
        while (executionQueue.isNotEmpty()) {
            executionQueue.removeFirst()()
        }
    }

    fun snap(surface: Surface, idSuffix: String? = null) {
        val id = testIdentifier + if (idSuffix != null) "_$idSuffix" else ""
        album.snap(surface, id)
    }

    private fun handleReport(report: SkijaTestAlbum.Report) {
        report.screenshots.forEach { (_, sReport) ->
            when (sReport.result) {
                ScreenshotResultProto.Status.PASSED -> {
                }

                ScreenshotResultProto.Status.MISSING_GOLDEN ->
                    throw AssertionError(
                        "Missing golden image " +
                            "'${sReport.locationOfGoldenInRepo}'. " +
                            "Did you mean to check in a new image?"
                    )
                else ->
                    throw AssertionError(
                        "Image mismatch! Expected image ${sReport
                            .expectedImageFileName}," +
                            " actual: ${sReport.currentScreenshotFileName}. FS" +
                            " location: ${config.fsGoldenPath}"
                    )
            }
        }
    }
}
