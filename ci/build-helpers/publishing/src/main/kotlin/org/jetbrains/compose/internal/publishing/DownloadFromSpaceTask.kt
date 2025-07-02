/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.publishing

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import java.net.URL

@Suppress("unused") // public api
abstract class DownloadFromSpaceMavenRepoTask : DefaultTask() {
    @get:Internal
    abstract val modulesToDownload: ListProperty<ModuleToUpload>

    @get:Internal
    abstract val spaceRepoUrl: Property<String>

    @TaskAction
    fun run() {
        for (module in modulesToDownload.get()) {
            downloadArtifactsFromComposeDev(module)
        }
    }

    private fun downloadArtifactsFromComposeDev(module: ModuleToUpload) {
        logger.info("Downloading ${module.coordinate}...")
        val groupUrl = module.groupId.replace(".", "/")

        val filesListingDocument =
            Jsoup.connect("${spaceRepoUrl.get()}/$groupUrl/${module.artifactId}/${module.version}/").get()
        val downloadableFiles = HashMap<String, URL>()
        for (a in filesListingDocument.select("#contents > a")) {
            val href = a.attributes().get("href")
            val lastPart = href.substringAfterLast("/", "")
            // check if URL points to a file
            if (lastPart.isNotEmpty() && lastPart.contains(".")) {
                downloadableFiles[lastPart] = URL(href)
            }
        }

        val destinationDir = module.localDir

        if (destinationDir.isFile)
            error("Destination dir is a file: $destinationDir")
        else if (destinationDir.exists()) {
            if (module.version.endsWith("-SNAPSHOT")) {
                destinationDir.deleteRecursively()
            } else {
                // delete existing files, that are not downloadable
                val existingFiles = (destinationDir.list() ?: emptyArray()).toSet()
                for (existingFileName in existingFiles) {
                    if (existingFileName !in downloadableFiles) {
                        destinationDir.resolve(existingFileName).delete()
                    }
                }
                // don't re-download all files for non-snapshot version
                val it = downloadableFiles.entries.iterator()
                while (it.hasNext()) {
                    val (fileName, _) = it.next()
                    if (fileName in existingFiles) {
                        it.remove()
                    }
                }
            }
        } else {
            destinationDir.mkdirs()
        }

        DownloadAction(project, this).apply {
            src(downloadableFiles.values)
            dest(destinationDir)
        }.execute()
    }
}