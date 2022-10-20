/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.tasks

import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.OutputFile
import java.io.File

abstract class ExtractXcodeGenTask : Copy() {

    @OutputFile
    fun getExecutable(): File {
        return rootSpec.destinationDir.resolve("xcodegen/bin/xcodegen")
    }

}