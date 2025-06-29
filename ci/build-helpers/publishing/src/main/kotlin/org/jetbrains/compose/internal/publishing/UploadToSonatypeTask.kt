/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.*
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.publishing.utils.*
import java.io.File
import java.util.*
import kotlin.io.readBytes

@Suppress("unused") // public api
abstract class UploadToSonatypeTask : DefaultTask() {
    // the task must always re-run anyway, so all inputs can be declared Internal

    @get:Internal
    abstract val user: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Internal
    abstract val deploymentName: Property<String>

    @get:Internal
    abstract val deploymentBundleFile: RegularFileProperty

    @TaskAction
    fun run() {
        runBlocking {
            HttpClient(CIO).use { client ->
                client.publish()
            }
        }
    }

    // By the doc https://central.sonatype.org/publish/publish-portal-api/
    private suspend fun HttpClient.publish() {
        val bearerToken = Base64.getEncoder().encode(
            "${user.get()}:${token.get()}".toByteArray()
        ).toString(Charsets.UTF_8)

        val response = submitForm {
            url("https://central.sonatype.com/api/v1/publisher/upload")
            parameter("name", deploymentName.get())
            parameter("publishingType", "USER_MANAGED")
            bearerAuth(bearerToken)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("bundle", deploymentBundleFile.get().readBytes(), headers {
                            append(HttpHeaders.ContentType, ContentType.Application.OctetStream.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"bundle.zip\"")
                        })
                    }
                )
            )
            onUpload { bytesSentTotal, contentLength ->
                logger.info("Sent $bytesSentTotal bytes from $contentLength")
            }
        }

        if (response.status != HttpStatusCode.Created) {
            error("Deployment failed (${response.status}):\n ${response.bodyAsText()}")
        }

        val deploymentId = response.bodyAsText().trim()

        logger.quiet("Deployment ID: $deploymentId")

        while (true) {
            logger.quiet("Checking Deployment Status: $deploymentId")

            val statusResponse = post {
                bearerAuth(bearerToken)
                accept(ContentType.Application.Json)
                url("https://central.sonatype.com/api/v1/publisher/status")
                parameter("id", deploymentId)
            }
            if (statusResponse.status != HttpStatusCode.OK) {
                error("Deployment failed (${statusResponse.status}):\n ${statusResponse.bodyAsText()}")
            }

            logger.quiet(statusResponse.bodyAsText())
            if (statusResponse.bodyAsText().contains("PUBLISHED")) break
            if (statusResponse.bodyAsText().contains("FAILED")) {
                error("Deployment failed (${statusResponse.status}):\n ${statusResponse.bodyAsText()}")
            }
        }
    }

    private fun validate(modules: List<ModuleToUpload>) {
        val validationIssues = arrayListOf<Pair<ModuleToUpload, ModuleValidator.Status.Error>>()
        for (module in modules) {
            val status = ModuleValidator(module).validate()
            if (status is ModuleValidator.Status.Error) {
                validationIssues.add(module to status)
            }
        }
        if (validationIssues.isNotEmpty()) {
            val message = buildString {
                appendLine("Some modules violate Maven Central requirements:")
                for ((module, status) in validationIssues) {
                    appendLine("* ${module.coordinate} (files: ${module.localDir})")
                    for (error in status.errors) {
                        appendLine("  * $error")
                    }
                }
            }
            error(message)
        }
    }
}