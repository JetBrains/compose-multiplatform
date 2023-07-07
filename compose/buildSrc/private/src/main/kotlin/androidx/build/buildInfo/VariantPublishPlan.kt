/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.buildInfo

import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider

/**
 * Info about a particular variant that will be published
 *
 * @param artifactId the maven artifact id
 * @param taskSuffix if non-null, will be added to the end of task names to disambiguate
 *                   (i.e. createLibraryBuildInfoFiles becomes createLibraryBuildInfoFilesJvm)
 * @param dependencies provider that will return the dependencies of this variant when/if needed
 */
data class VariantPublishPlan(
    val artifactId: String,
    val taskSuffix: String = "",
    val dependencies: Provider<List<Dependency>>
)