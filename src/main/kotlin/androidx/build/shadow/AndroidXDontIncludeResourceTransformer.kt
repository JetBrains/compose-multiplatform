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

package androidx.build.shadow

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.gradle.api.file.FileTreeElement
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import shadow.org.apache.tools.zip.ZipOutputStream

/**
 * Copy of upstream DontIncludeResourceTransformer that has a fix for Gradle 7.0
 * https://github.com/johnrengelman/shadow/blob/master/src/main/groovy/com/github/jengelman/gradle/plugins/shadow/transformers/DontIncludeResourceTransformer.groovy
 */
class AndroidXDontIncludeResourceTransformer : Transformer {
    @get:[Input Optional]
    var resource: String? = null
    override fun canTransformResource(element: FileTreeElement?): Boolean {
        val path = element?.relativePath?.pathString ?: return false
        val resourceSuffix = resource ?: return false
        if (resourceSuffix.isNotEmpty() && path.endsWith(resourceSuffix)) {
            return true
        }
        return false
    }

    override fun transform(context: TransformerContext?) {
        // No-op
    }

    override fun hasTransformedResource(): Boolean = false

    override fun modifyOutputStream(p0: ZipOutputStream?, p1: Boolean) {
        // No-op
    }
}
