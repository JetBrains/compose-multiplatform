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

package androidx.build

import androidx.build.gradle.isRoot
import org.gradle.api.Project

/**
 * Creates a dependency substitution rule to workaround
 * [a Gradle bug](https://github.com/gradle/gradle/issues/20778).
 *
 * The root cause of the bug is a mix of external and project coordinates existing
 * simultaneously in the dependency graph. A Gradle optimization attempts to simplify/minimize
 * this graph to allow artifact transforms to being executing as soon as possible, but the
 * optimization was too aggressive in the Androidx case.
 *
 * This workaround creates a no-op/unmatching rule which invalidates the above optimization and
 * prevents transformations from executing too eagerly.
 *
 * This is necessary for Gradle 7.5-rc-1, but should be fixed in Gradle 7.5.1 or 7.6, at which
 * point this class can be removed.
 */
object GradleTransformWorkaround {
    /**
     * This function applies the [GradleTransformWorkaround] to the given root project, if
     * necessary (if it includes lifecycle-common).
     *
     * @param rootProject The root project whose sub-projects will be updated with the
     *        workaround.
     */
    fun maybeApply(rootProject: Project) {
        check(rootProject.isRoot) {
            """
                GradleTransformWorkaround must be invoked with the root project
                because it needs to be applied to all sub-projects.
            """.trimIndent()
        }
        rootProject.subprojects { subProject ->
            if (subProject.path == ":lifecycle:lifecycle-common") {
                rootProject.subprojects {
                    it.applyArtifactTransformWorkaround()
                }
            }
        }
    }

    private fun Project.applyArtifactTransformWorkaround() {
        this.configurations.all { c ->
            c.resolutionStrategy.dependencySubstitution { selector ->
                selector.substitute(selector.module("unmatched:unmatched"))
                    .using(selector.project(":lifecycle:lifecycle-common"))
                    .because("workaround gradle/gradle#20778 with intentionally unmatching " +
                        "substitution rule")
            }
        }
    }
}
