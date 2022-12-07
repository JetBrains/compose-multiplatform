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
import org.tomlj.TomlTable

/**
 * Loads Library groups and versions from a specified TOML file.
 */
abstract class LibraryVersionsService : BuildService<LibraryVersionsService.Parameters> {
    interface Parameters : BuildServiceParameters {
        var tomlFile: Provider<String>
        var composeCustomVersion: Provider<String>
        var composeCustomGroup: Provider<String>
        var useMultiplatformGroupVersions: Provider<Boolean>
    }

    private val parsedTomlFile: TomlParseResult by lazy {
        Toml.parse(parameters.tomlFile.get())
    }

    private fun getTable(key: String): TomlTable {
        return parsedTomlFile.getTable(key)
            ?: throw GradleException("Library versions toml file is missing [$key] table")
    }

    // map from name of constant to Version
    val libraryVersions: Map<String, Version> by lazy {
        val versions = getTable("versions")
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

    // map of library groups keyed by their variable name in the toml file
    val libraryGroups: Map<String, LibraryGroup> by lazy {
        val result = mutableMapOf<String, LibraryGroup>()
        for (association in libraryGroupAssociations) {
          result.put(association.declarationName, association.libraryGroup)
        }
        result
    }

    // map of library groups keyed by group name
    val libraryGroupsByGroupId: Map<String, LibraryGroup> by lazy {
        val result = mutableMapOf<String, LibraryGroup>()
        for (association in libraryGroupAssociations) {
          result.put(association.libraryGroup.group, association.libraryGroup)
        }
        result
    }

    // map from project name to group override if applicable
    val overrideLibraryGroupsByProjectPath: Map<String, LibraryGroup> by lazy {
       val result = mutableMapOf<String, LibraryGroup>()
       for (association in libraryGroupAssociations) {
           for (overridePath in association.overrideIncludeInProjectPaths) {
               result.put(overridePath, association.libraryGroup)
           }
       }
       result
    }

    private val libraryGroupAssociations: List<LibraryGroupAssociation> by lazy {
        val groups = getTable("groups")
        val useMultiplatformGroupVersion =
            parameters.useMultiplatformGroupVersions.orElse(false).get()

        fun readGroupVersion(groupDefinition: TomlTable, groupName: String, key: String): Version? {
            val versionRef = groupDefinition.getString(key) ?: return null
            if (!versionRef.startsWith(VersionReferencePrefix)) {
                throw GradleException(
                    "Group entry $key is expected to start with $VersionReferencePrefix"
                )
            }
            // name without `versions.`
            val atomicGroupVersionName = versionRef.removePrefix(
                VersionReferencePrefix
            )
            return libraryVersions[atomicGroupVersionName] ?: error(
                "Group entry $groupName specifies $atomicGroupVersionName, but such version " +
                    "doesn't exist"
            )
        }
        val result = mutableListOf<LibraryGroupAssociation>()
        for (name in groups.keySet()) {
            // get group name
            val groupDefinition = groups.getTable(name)!!
            val groupName = groupDefinition.getString("group")!!
            val finalGroupName = if (name.startsWith("COMPOSE") &&
                parameters.composeCustomGroup.isPresent
            ) {
                groupName.replace("androidx.compose", parameters.composeCustomGroup.get())
            } else groupName

            // get group version, if any
            val atomicGroupVersion = readGroupVersion(
                groupDefinition = groupDefinition,
                groupName = groupName,
                key = AtomicGroupVersion
            )
            val multiplatformGroupVersion = readGroupVersion(
                groupDefinition = groupDefinition,
                groupName = groupName,
                key = MultiplatformGroupVersion
            )
            check(
                multiplatformGroupVersion == null || atomicGroupVersion != null
            ) {
                "Cannot specify $MultiplatformGroupVersion for $name without specifying an " +
                    AtomicGroupVersion
            }
            val groupVersion = when {
                useMultiplatformGroupVersion -> multiplatformGroupVersion ?: atomicGroupVersion
                else -> atomicGroupVersion
            }

            val overrideApplyToProjects = (
                groupDefinition.getArray("overrideInclude")?.toList() ?: listOf()
            ).map({ it -> it as String })

            val group = LibraryGroup(finalGroupName, groupVersion)
            val association = LibraryGroupAssociation(name, group, overrideApplyToProjects)
            result.add(association)
        }
        result
    }
}

// a LibraryGroupSpec knows how to associate a LibraryGroup with the appropriate projects
data class LibraryGroupAssociation(
    // the name of the variable to which it is assigned in the toml file
    val declarationName: String,
    // the group
    val libraryGroup: LibraryGroup,
    // the paths of any additional projects that this group should be assigned to
    val overrideIncludeInProjectPaths: List<String>
)

private const val VersionReferencePrefix = "versions."
private const val AtomicGroupVersion = "atomicGroupVersion"
private const val MultiplatformGroupVersion = "multiplatformGroupVersion"