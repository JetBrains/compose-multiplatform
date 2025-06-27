/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing.utils

import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import org.jetbrains.compose.internal.publishing.ModuleToUpload

interface SonatypeApi {
    fun upload(repo: StagingRepo, module: ModuleToUpload)
    fun stagingProfiles(): StagingProfiles
    fun createStagingRepo(profile: StagingProfile, description: String): StagingRepo
    fun closeStagingRepo(repo: StagingRepo)
}

@JsonRootName("stagingProfile")
data class StagingProfile(
    var id: String = "",
    var name: String = "",
)

@JsonRootName("stagingProfiles")
class StagingProfiles(
    @JacksonXmlElementWrapper
    var data: List<StagingProfile>
)

data class StagingRepo(
    val id: String,
    val description: String,
    val profile: StagingProfile
) {
    constructor(
        response: PromoteResponse,
        profile: StagingProfile
    ) : this(
        id = response.data.stagedRepositoryId!!,
        description = response.data.description,
        profile = profile
    )

    @JsonRootName("promoteRequest")
    data class PromoteRequest(var data: PromoteData)
    @JsonRootName("promoteResponse")
    data class PromoteResponse(var data: PromoteData)
    data class PromoteData(var stagedRepositoryId: String? = null, var description: String)
}
