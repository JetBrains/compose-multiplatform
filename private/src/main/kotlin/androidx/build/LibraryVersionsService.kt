/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build

import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.tomlj.Toml
import org.tomlj.TomlParseResult

/**
 * Loads Library groups and versions from a specified TOML file.
 */
abstract class LibraryVersionsService : BuildService<LibraryVersionsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        var tomlFile: Provider<String>
        var composeCustomVersion: Provider<String>
        var composeCustomGroup: Provider<String>
    }

    private val parsedTomlFile: TomlParseResult by lazy {
        Toml.parse(parameters.tomlFile.get())
    }

    val libraryVersions: Map<String, Version> by lazy {
        val versions = parsedTomlFile.getTable("versions")
            ?: throw GradleException("Library versions toml file is missing [versions] table")
        versions.keySet().associateWith { versionName ->
            val versionValue =
                if (versionName.startsWith("COMPOSE") &&
                    parameters.composeCustomVersion.isPresent
                ) {
                    parameters.composeCustomVersion.get()
                } else {
                    versions.getString(versionName)!!
                }
            Version.parseOrNull(versionValue)
                ?: throw GradleException(
                    "$versionName does not match expected format - $versionValue"
                )
        }
    }

    val libraryGroups: Map<String, LibraryGroup> by lazy {
        val groups = parsedTomlFile.getTable("groups")
            ?: throw GradleException("Library versions toml file is missing [groups] table")
        groups.keySet().associateWith { name ->
            val groupDefinition = groups.getTable(name)!!
            val groupName = groupDefinition.getString("group")!!
            val finalGroupName = if (name.startsWith("COMPOSE") &&
                parameters.composeCustomGroup.isPresent
            ) {
                groupName.replace("androidx.compose", parameters.composeCustomGroup.get())
            } else groupName

            if (groupDefinition.contains(AtomicGroupVersion)) {
                val atomicGroupVersionReference = groupDefinition.getString(AtomicGroupVersion)!!
                if (!atomicGroupVersionReference.startsWith(VersionReferencePrefix)) {
                    throw GradleException(
                        "Group entry $AtomicGroupVersion is expected to start with " +
                            VersionReferencePrefix
                    )
                }
                // name without `versions.`
                val atomicGroupVersionName = atomicGroupVersionReference.removePrefix(
                    VersionReferencePrefix
                )
                check(libraryVersions.containsKey(atomicGroupVersionName)) {
                    "Group entry $name specifies $atomicGroupVersionName, but such version " +
                        "doesn't exist"
                }
                LibraryGroup(finalGroupName, libraryVersions[atomicGroupVersionName])
            } else {
                LibraryGroup(finalGroupName, null)
            }
        }
    }
}

private const val VersionReferencePrefix = "versions."
private const val AtomicGroupVersion = "atomicGroupVersion"