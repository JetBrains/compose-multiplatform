/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.*
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.headers
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.publishing.utils.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("unused") // public api
abstract class UploadToSonatypeTask : DefaultTask() {
    // the task must always re-run anyway, so all inputs can be declared Internal

    @get:Internal
    abstract val user: Property<String>

    @get:Internal
    abstract val password: Property<String>

    @get:Internal
    abstract val deployName: Property<String>

    @get:Internal
    abstract val publishAfterUploading: Property<Boolean>

    @get:Internal
    abstract val modulesToUpload: ListProperty<ModuleToUpload>

    @TaskAction
    fun run() {
        val deploymentBundle = createDeploymentBundle(modulesToUpload.get())
        runBlocking {
            HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 5 * 60 * 1000 // 5 minutes
                    connectTimeoutMillis = 60 * 1000     // 1 minute
                    socketTimeoutMillis = 5 * 60 * 1000  // 5 minutes
                }
                install(HttpRequestRetry) {
                    retryOnExceptionOrServerErrors(maxRetries = 5)
                    exponentialDelay()
                }
            }.use { client ->
                client.publish(deploymentBundle)
            }
        }
    }

    private fun createDeploymentBundle(modules: List<ModuleToUpload>): InputProvider {
        val zipFile = project.buildDir.resolve("publishing/compose-deploy.zip")
        zipFile.parentFile.mkdirs()

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            val sourcesToDestinations = modules.map { it.localDir to it.mavenDirectory() }
            val addedEntries = mutableSetOf<String>()

            for ((sourceDir, destDir) in sourcesToDestinations) {
                val files = sourceDir.listFiles() ?: continue

                for (file in files) {
                    val entryPath = "$destDir/${file.name}"
                    if (file.isFile && !addedEntries.contains(entryPath)) {
                        addedEntries.add(entryPath)
                        val entry = ZipEntry(entryPath)
                        zipOut.putNextEntry(entry)
                        file.inputStream().use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        }

        logger.info("Zip bundle is created at $zipFile")

        return InputProvider(zipFile.length()) {
            FileInputStream(zipFile).asInput()
        }
    }

    // By the doc https://central.sonatype.org/publish/publish-portal-api/
    private suspend fun HttpClient.publish(deploymentBundle: InputProvider) {
        val publishAfterUploading = publishAfterUploading.get()

        val bearerToken = Base64.getEncoder().encode(
            "${user.get()}:${password.get()}".toByteArray()
        ).toString(Charsets.UTF_8)

        logger.info("Start uploading ${deployName.get()}")

        val response = submitForm {
            url("https://central.sonatype.com/api/v1/publisher/upload")
            parameter("name", deployName.get())
            parameter("publishingType", if (publishAfterUploading) "AUTOMATIC" else "USER_MANAGED")
            bearerAuth(bearerToken)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("bundle", deploymentBundle, headers {
                            append(HttpHeaders.ContentType, ContentType.Application.OctetStream.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"bundle.zip\"")
                        })
                    }
                )
            )
            var lastUploadLogTime = 0L
            onUpload { bytesSentTotal, contentLength ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUploadLogTime >= 5000) { // 5 seconds debounce
                    logger.info("Sent $bytesSentTotal bytes from $contentLength")
                    lastUploadLogTime = currentTime
                }
            }
        }

        if (response.status != HttpStatusCode.Created) {
            error("Deployment failed (${response.status}):\n ${response.bodyAsText()}")
        }

        val deploymentId = response.bodyAsText().trim()
        logger.info("Successfully uploaded ${deploymentId.take(4)}")

        val endStatus = if (publishAfterUploading) "PUBLISHED" else "VALIDATED"

        while (true) {
            logger.info("Checking the status of the deployment...")

            val statusResponse = post {
                bearerAuth(bearerToken)
                accept(ContentType.Application.Json)
                url("https://central.sonatype.com/api/v1/publisher/status")
                parameter("id", deploymentId)
            }

            if (statusResponse.status != HttpStatusCode.OK) {
                error("Deployment failed (${statusResponse.status}):\n ${statusResponse.bodyAsText()}")
            }

            if (statusResponse.bodyAsText().contains(endStatus)) break
            if (statusResponse.bodyAsText().contains("FAILED")) {
                error("Deployment failed (${statusResponse.status}):\n ${statusResponse.bodyAsText()}")
            }

            delay(5000)
        }

        logger.info("Successfully published")
    }

    private fun ModuleToUpload.mavenDirectory() =
        groupId.replace(".", "/") + "/" + artifactId + "/" + version
}
