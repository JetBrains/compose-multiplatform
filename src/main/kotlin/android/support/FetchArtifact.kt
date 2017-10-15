/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support

import org.gradle.api.GradleException
import java.io.File

const val FETCH_ARTIFACT = "/google/data/ro/projects/android/fetch_artifact"

/**
 * fetch artifact from the build server it is similar to run in command line:
 * /google/data/ro/projects/android/fetch_artifact --bid <buildId> --target <target> path
 */
fun fetch(workingDir: File, buildId: Int, target: String, path: String) = ioThread {
    if (!File(FETCH_ARTIFACT).exists()) {
        throw GradleException("$FETCH_ARTIFACT doesn't exist")
    }

    val process = ProcessBuilder(FETCH_ARTIFACT, "--bid", "$buildId", "--target", target, path)
            .directory(workingDir)
            .start()

    process.awaitSuccess("Fetch artifact")
    File(workingDir, path)
}
