/*
 * Copyright (C) 2019 The Android Open Source Project
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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Task that checks that all libraries within groups with requireSameVersion set to true
 * are actually of the same version.
 */
open class CheckSameVersionLibraryGroupsTask : DefaultTask() {

    @TaskAction
    fun checkSameVersionLibraryGroups() {
        val map = HashMap<String, Pair<String, String>>()
        project.subprojects { project ->
            val library =
                project.extensions.findByType(AndroidXExtension::class.java) ?: return@subprojects
            val requireSameVersion = library.mavenGroup?.requireSameVersion ?: false
            if (requireSameVersion && library.publish.shouldRelease()) {
                if (!project.isVersionSet()) {
                    throw GradleException("Library $group:${project.name} does not specify " +
                            "a version, however it is within library group $group which requires" +
                            " all member libraries to be of the same version")
                }
                val group = library.mavenGroup!!.group
                if (map.contains(group)) {
                    val existingVersion = map.get(group)!!.first
                    val libraryInSameGroup = map.get(group)!!.second
                    if (existingVersion != project.version.toString()) {
                        throw GradleException("Library $group:${project.name} with version " +
                                "${project.version} is part of the $group library group" +
                                " which requires all member libraries to have the same " +
                                "version, however library $group:$libraryInSameGroup within" +
                                " the same library group has different version $existingVersion")
                    }
                } else {
                    map.put(group, Pair(project.version.toString(), project.name))
                }
            }
        }
    }
}
