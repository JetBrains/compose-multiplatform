/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.dependencyTracker

import org.gradle.api.Project
import java.io.File

/**
 * Creates a project graph for fast lookup by file path
 */
class ProjectGraph(rootProject: Project) {
    private val rootNode: Node

    init {
        rootNode = Node()
        rootProject.subprojects.forEach {
            val relativePath = it.projectDir.toRelativeString(rootProject.projectDir)
            val sections = relativePath.split(File.separatorChar)
            val leaf = sections.fold(rootNode) { left, right ->
                left.getOrCreateNode(right)
            }
            leaf.project = it
        }
    }

    /**
     * Finds the project that contains the given file.
     * The file's path prefix should match the project's path.
     */
    fun findContainingProject(filePath: String): Project? {
        val sections = filePath.split(File.separatorChar)
        return rootNode.find(sections, 0)
    }

    private class Node {
        var project: Project? = null
        private val children = mutableMapOf<String, Node>()

        fun getOrCreateNode(key: String): Node {
            return children.getOrPut(key) { Node() }
        }

        fun find(sections: List<String>, index: Int): Project? {
            if (sections.size <= index) {
                return project
            }
            val child = children[sections[index]]
            return if (child == null) {
                project
            } else {
                child.find(sections, index + 1)
            }
        }
    }
}