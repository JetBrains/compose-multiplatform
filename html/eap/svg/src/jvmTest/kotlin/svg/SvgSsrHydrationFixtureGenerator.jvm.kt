/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.svg

import java.io.File
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.composeHtmlToString

@OptIn(ExperimentalComposeWebApi::class, ExperimentalComposeWebSvgApi::class)
internal object SvgSsrHydrationFixtureGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputDirectory = File(requireNotNull(args.singleOrNull()) {
            "Expected the output fixture directory"
        })

        outputDirectory.mkdirs()
        outputDirectory.resolve("svg-ssr-hydration.html").writeText(
            "\n    ${composeHtmlToString {
                SvgSsrHydrationContent(count = 0, increment = {})
            }}\n",
        )
    }
}
