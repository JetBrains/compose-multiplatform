/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.uptodatedness

import java.io.File
import org.gradle.api.Task

// Tells Gradle to skip running this task, even if this task declares no output files
fun Task.cacheEvenIfNoOutputs() {
    this.outputs.file(this.getDummyOutput())
}

// Returns a dummy/unused output path that we can pass to Gradle to prevent Gradle from thinking
// that we forgot to declare outputs of this task, and instead to skip this task if its inputs
// are unchanged
fun Task.getDummyOutput(): File {
    return File(this.project.buildDir, "dummyOutput/" + this.name.replace(":", "-"))
}
