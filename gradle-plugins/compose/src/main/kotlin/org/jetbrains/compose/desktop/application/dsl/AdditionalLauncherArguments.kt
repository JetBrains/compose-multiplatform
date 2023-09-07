/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

abstract class AdditionalLauncherArguments {
    private val options: MutableList<String> = mutableListOf()

    fun add(key: String, value: String) {
        if (ValidKeys.contains(key)) {
            options.add("$key=$value")
        } else {
            error("Key \"$key\" is invalid. Valid keys are: ${ValidKeys.joinToString(", ")}")
        }
    }

    internal fun getFileContent(): String {
        return options.joinToString("\n")
    }

    companion object {
        val ValidKeys = listOf(
            "module", "main-jar", "main-class", "description", "arguments", "java-options", "app-version",
            "icon", "launcher-as-service", "win-console", "win-shortcut", "win-menu", "linux-app-category",
            "linux-shortcut"
        )
    }
}