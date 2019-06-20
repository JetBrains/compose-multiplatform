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

import androidx.build.SupportConfig
import org.gradle.api.Project
import java.io.File

import org.gradle.api.logging.Logger

/**
 * Creates a project graph for fast lookup by file path
 */
class ProjectGraph(rootProject: Project, val logger: Logger? = null) {
    private val rootNode: Node

    init {
        // always use cannonical file: b/112205561
        logger?.info("initializing ProjectGraph")
        rootNode = Node(logger)
        val rootProjectDir = SupportConfig.getSupportRoot(rootProject).canonicalFile
        rootProject.subprojects.forEach {
            logger?.info("creating node for ${it.path}")
            val relativePath = it.projectDir.canonicalFile.toRelativeString(rootProjectDir)
            val sections = relativePath.split(File.separatorChar)
            logger?.info("relative path: $relativePath , sections: $sections")
            val leaf = sections.fold(rootNode) { left, right ->
                left.getOrCreateNode(right)
            }
            leaf.project = it
        }
        logger?.info("finished creating ProjectGraph")
    }

    /**
     * Finds the project that contains the given file.
     * The file's path prefix should match the project's path.
     */
    fun findContainingProject(filePath: String): Project? {
        val sections = filePath.split(File.separatorChar)
        logger?.info("finding containing project for $filePath , sections: $sections")
        return rootNode.find(sections, 0)
    }

    private class Node(val logger: Logger? = null) {
        var project: Project? = null
        private val children = mutableMapOf<String, Node>()

        fun getOrCreateNode(key: String): Node {
            return children.getOrPut(key) { Node(logger) }
        }

        fun find(sections: List<String>, index: Int): Project? {
            logger?.info("finding $sections with index $index in ${project?.path ?: "root"}")
            if (sections.size <= index) {
                logger?.info("nothing")
                return project
            }
            val child = children[sections[index]]
            return if (child == null) {
                logger?.info("no child found, returning ${project?.path ?: "root"}")
                project
            } else {
                child.find(sections, index + 1)
            }
        }
    }
}
