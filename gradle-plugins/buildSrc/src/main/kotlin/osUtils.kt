/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

enum class OS(val id: String) {
    Linux("linux"),
    Windows("windows"),
    MacOS("macos")
}

val hostOS: OS by lazy {
    val osName = System.getProperty("os.name")
    when {
        osName == "Mac OS X" -> OS.MacOS
        osName == "Linux" -> OS.Linux
        osName.startsWith("Win") -> OS.Windows
        else -> throw Error("Unknown OS $osName")
    }
}