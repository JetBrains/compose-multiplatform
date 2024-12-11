/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.runBlocking
import space.jetbrains.api.runtime.*
import space.jetbrains.api.runtime.resources.projects
import space.jetbrains.api.runtime.types.*

internal class SpaceApiClient(
    private val serverUrl: String,
    private val clientId: String,
    private val clientSecret: String,
) {
    data class PackageInfo(
        val groupId: String,
        val artifactId: String,
        val version: String
    ) {
        override fun toString() = "$groupId:$artifactId:$version"
    }

    fun forEachPackageWithVersion(
        projectId: ProjectIdentifier,
        repoId: PackageRepositoryIdentifier,
        version: String,
        fn: (PackageInfo) -> Unit
    ) {
        withSpaceClient {
            forEachPackage(projectId, repoId) { pkg ->
                val details = projects.packages.repositories.packages.versions
                    .getPackageVersionDetails(
                        projectId, repoId, pkg.name, version
                    )
                if (details != null) {
                    val split = pkg.name.split("/")
                    if (split.size != 2) {
                        error("Invalid maven package name: '${pkg.name}'")
                    }
                    fn(PackageInfo(groupId = split[0], artifactId = split[1], version = version))
                }
            }
        }
    }

    private fun withSpaceClient(fn: suspend SpaceHttpClientWithCallContext.() -> Unit) {
        runBlocking {
            HttpClient(OkHttp).use { client ->
                val space = SpaceHttpClient(client).withServiceAccountTokenSource(
                    serverUrl = serverUrl,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
                space.fn()
            }
        }
    }

    private fun batches(batchSize: Int = 100) =
        generateSequence(0) { it + batchSize }
            .map { BatchInfo(it.toString(), batchSize) }

    private suspend fun <T> forAllInAllBatches(
        getBatch: suspend (BatchInfo) -> Batch<T>,
        fn: suspend (T) -> Unit
    ) {
        for (batchInfo in batches()) {
            val batch = getBatch(batchInfo)

            for (element in batch.data) {
                fn(element)
            }

            if (batch.data.isEmpty() || (batch.next.toIntOrNull() ?: 0) >= (batch.totalCount ?: 0)) return
        }
    }

    private suspend fun SpaceHttpClientWithCallContext.forEachPackage(
        projectId: ProjectIdentifier,
        repoId: PackageRepositoryIdentifier,
        fn: suspend (PackageData) -> Unit
    ) {
        forAllInAllBatches({ batch ->
            projects.packages.repositories.packages.getAllPackages(
                project = projectId,
                repository = repoId,
                query = "",
                batchInfo = batch
            )
        }, fn)
    }
}