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

package androidx.build.resources

import androidx.build.getSupportRootFolder
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

fun Project.configurePublicResourcesStub(extension: LibraryExtension) {
    val targetResFolder = File(project.buildDir, "generated/res/public-stub")

    val generatePublicResourcesTask = tasks.register(
        "generatePublicResourcesStub",
        Copy::class.java
    ) { task ->
        task.from(File(project.getSupportRootFolder(), "buildSrc/res"))
        task.into(targetResFolder)
    }

    extension.libraryVariants.all { variant ->
        variant.registerGeneratedResFolders(
            project.files(targetResFolder).builtBy(generatePublicResourcesTask)
        )
    }
}
