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

import androidx.build.dependencyTracker.AffectedModuleDetector
import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import org.gradle.api.file.ConfigurableFileCollection

private const val HASH_ALGORITHM = "SHA-256"
private const val TXT_FILE_EXTENSION = ".txt"

@CacheableTask
abstract class ApkHashDump : DefaultTask() {

    init {
        group = "LibraryMetrics"
        description =
            "Task for dumping APK hashes for tests in order to track efficacy of test caching."
    }

    /**
     * The variants we are interested in gathering apk hashes for.
     */
    @get:[InputFiles PathSensitive(PathSensitivity.NONE)]
    abstract val apkDir: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: Property<File>

    @TaskAction
    fun dumpApkHash() {
        val file = outputFile.get()
        file.parentFile.mkdirs()

        val apkFile = getApkFile()

        val hash = MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(apkFile.readBytes())
            .fold("") { str, it -> str + "%02x".format(it) }

        file.writeText(hash)
    }

    private fun getApkFile(): File {
        return apkDir.files.first().listFiles().single {
            it.name.endsWith(".apk")
        }
    }
}

fun Project.addToApkHashDump(packageTask: PackageAndroidArtifact) {
    val task = tasks.register(
        "${AndroidXImplPlugin.APK_HASH_DUMP}_${packageTask.variantName}",
        ApkHashDump::class.java
    )
    task.configure {
        val outputDir = project.rootProject.getApkHashDumpDirectory()
        it.outputFile.set(
            task.map {
                File(outputDir,
       "${project.group}_${project.name}_${packageTask.variantName}_hash$TXT_FILE_EXTENSION")
            }
        )
        it.apkDir.from(packageTask.outputDirectory)
        AffectedModuleDetector.configureTaskGuard(it)
    }
    addToBuildOnServer(task)
}
