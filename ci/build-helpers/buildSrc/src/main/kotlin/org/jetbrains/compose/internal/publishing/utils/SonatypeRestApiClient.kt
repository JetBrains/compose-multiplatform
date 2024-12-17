/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.apache.tika.Tika
import org.gradle.api.logging.Logger
import org.jetbrains.compose.internal.publishing.ModuleToUpload
import java.io.Closeable
import java.io.File

// https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API
class SonatypeRestApiClient(
    sonatypeServer: String,
    user: String,
    password: String,
    private val logger: Logger,
) : SonatypeApi, Closeable {
    private val client = RestApiClient(sonatypeServer, user, password, logger)

    private fun buildRequest(urlPath: String, builder: Request.Builder.() -> Unit): Request =
        client.buildRequest(urlPath, builder)

    private fun <T> Request.execute(processResponse: (ResponseBody) -> T): T =
        client.execute(this, processResponse = processResponse)

    override fun close() {
        client.close()
    }

    override fun upload(repo: StagingRepo, module: ModuleToUpload) {
        for (file in module.localDir.listFiles()!!) {
            uploadFile(repo, module, file)
        }
    }

    private fun uploadFile(repo: StagingRepo, module: ModuleToUpload, file: File) {
        val fileType = Tika().detect(file.name)
        logger.info("Uploading $file (detected type='$fileType', length=${file.length()})")
        val deployUrl = "service/local/staging/deployByRepositoryId/${repo.id}"
        val groupUrl = module.groupId.replace(".", "/")
        val coordinateUrl = "$groupUrl/${module.artifactId}/${module.version}"
        val uploadUrlPath = "$deployUrl/$coordinateUrl/${file.name}"

        buildRequest(uploadUrlPath) {
            header("Content-type", fileType)
            put(file.asRequestBody(fileType.toMediaTypeOrNull()))
        }.execute { }
    }

    override fun stagingProfiles(): StagingProfiles =
        buildRequest("service/local/staging/profiles") {
            get()
        }.execute { responseBody ->
            Xml.deserialize(responseBody.string())
        }

    override fun createStagingRepo(profile: StagingProfile, description: String): StagingRepo {
        logger.info("Creating sonatype staging repository for `${profile.id}` with description `$description`")
        val response =
            buildRequest("service/local/staging/profiles/${profile.id}/start") {
                val promoteRequest = StagingRepo.PromoteRequest(
                    StagingRepo.PromoteData(description = description)
                )
                post(Xml.serialize(promoteRequest).toRequestBody(Xml.mediaType))
            }.execute { responseBody ->
                Xml.deserialize<StagingRepo.PromoteResponse>(responseBody.string())
            }
        return StagingRepo(response, profile)
    }

    override fun closeStagingRepo(repo: StagingRepo) {
        logger.info("Closing repository '${repo.id}'")
        buildRequest("service/local/staging/bulk/close") {
            val request = "{\"data\":{\"stagedRepositoryIds\":[\"${repo.id}\"]}}"
            post(request.toRequestBody(Json.mediaType))
                .addHeader("Accept", Json.mediaType.toString())
        }.execute { responseBody ->
            logger.info("Finished closing repository '${repo.id}': '${responseBody.string()}'")
        }
    }
}
