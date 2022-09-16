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

package androidx.build.transform

import com.android.build.api.attributes.BuildTypeAttr
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage

/**
 * Creates `testAarAsJar` configuration that can be used for JVM tests that need to Android library
 * classes on the classpath.
 */
fun configureAarAsJarForConfiguration(project: Project, configurationName: String) {
    val testAarsAsJars = project.configurations.create("${configurationName}AarAsJar") {
        it.isTransitive = false
        it.isCanBeConsumed = false
        it.isCanBeResolved = true
        it.attributes.attribute(
            BuildTypeAttr.ATTRIBUTE,
            project.objects.named(BuildTypeAttr::class.java, "release")
        )
        it.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            project.objects.named(Usage::class.java, Usage.JAVA_API)
        )
    }
    val artifactType = Attribute.of("artifactType", String::class.java)
    project.dependencies.registerTransform(IdentityTransform::class.java) { spec ->
        spec.from.attribute(artifactType, "jar")
        spec.to.attribute(artifactType, "aarAsJar")
    }

    project.dependencies.registerTransform(ExtractClassesJarTransform::class.java) { spec ->
        spec.from.attribute(artifactType, "aar")
        spec.to.attribute(artifactType, "aarAsJar")
    }

    val aarAsJar = testAarsAsJars.incoming.artifactView { viewConfiguration ->
        viewConfiguration.attributes.attribute(artifactType, "aarAsJar")
    }.files
    project.configurations.getByName(configurationName).dependencies.add(
        project.dependencies.create(aarAsJar)
    )
}