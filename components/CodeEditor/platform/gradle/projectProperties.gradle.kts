/*
 * Copyright 2017-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Modified by Alex Hosh (n34to0@gmail.com) 2021.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import java.util.*
import java.io.*

val scriptDirectory: File = File(buildscript.sourceURI!!.rawPath).parentFile
val propertiesFile: File = File(scriptDirectory, "project.properties")

FileReader(propertiesFile).use {
    val properties = Properties()
    properties.load(it)
    properties.forEach { (k, v) ->
        extra[k.toString()] = v
    }
}
