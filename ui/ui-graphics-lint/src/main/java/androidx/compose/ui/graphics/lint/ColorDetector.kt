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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.graphics.lint

import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.kotlin.KotlinULiteralExpression
import java.util.EnumSet

/**
 * [Detector] that checks hex Color definitions to ensure that they provide values for all four
 * (ARGB) channels. Providing only three channels (such as 0xFF0000) will result in an empty
 * alpha channel, which is rarely intended - in cases where it is, it is typically more readable
 * to just explicitly define the alpha channel anyway.
 */
class ColorDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(Names.UiGraphics.Color.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Names.UiGraphics.PackageName)) {
            // Ignore other Color functions that have separate parameters for separate channels
            if (node.valueArgumentCount == 1) {
                val argument = node.valueArguments.first()
                // Ignore non-literal expressions
                if (argument !is KotlinULiteralExpression) return
                val argumentText = argument.sourcePsi.text ?: return
                val hexPrefix = "0x"
                val hexIndex = argumentText.indexOf(hexPrefix, ignoreCase = true)
                // Ignore if this isn't a hex value
                if (hexIndex != 0) return
                val hexArgument = argumentText.substring(hexIndex + hexPrefix.length)
                // The length of the actual hex value (without separators and suffix) should be 8
                val hexLength = hexArgument
                    // Trim any underscores that might be used to separate values
                    .replace("_", "")
                    // Remove the suffix `L` if present
                    .replace("L", "")
                    .length
                when (hexLength) {
                    // Expected length is 8: four 8-bit channels, e.g FF000000
                    8 -> return
                    // In the specific (and common) case of missing the alpha channel, i.e
                    // FF0000, we can suggest a quick fix to add a value of `FF` for the alpha,
                    // and a more specific lint warning.
                    6 -> {
                        // Try to be consistent with how the hex value is currently defined - if
                        // there are any lower case characters, suggest to add a lower case
                        // channel. Otherwise use upper case as the default.
                        val isHexValueLowerCase = hexArgument
                            .firstOrNull {
                                !it.isDigit()
                            }?.isLowerCase() == true

                        val alphaChannel = if (isHexValueLowerCase) "ff" else "FF"
                        val replacement = hexPrefix + alphaChannel + hexArgument

                        context.report(
                            MissingColorAlphaChannel,
                            node,
                            context.getLocation(argument),
                            "Missing Color alpha channel",
                            LintFix.create()
                                .replace()
                                .name("Add `$alphaChannel` alpha channel")
                                .text(argumentText)
                                .with(replacement)
                                .autoFix()
                                .build()
                        )
                    }
                    // Otherwise report a generic warning for an valid value - there is no quick fix
                    // we can really provide here.
                    else -> {
                        context.report(
                            InvalidColorHexValue,
                            node,
                            context.getLocation(argument),
                            "Invalid Color hex value",
                        )
                    }
                }
            }
        }
    }

    companion object {
        val MissingColorAlphaChannel = Issue.create(
            "MissingColorAlphaChannel",
            "Missing Color alpha channel",
            "Creating a Color with a hex value requires a 32 bit value " +
                "(such as 0xFF000000), with 8 bits being used per channel (ARGB). Not passing a " +
                "full 32 bit value will result in channels being undefined. For example, passing " +
                "0xFF0000 will result in a missing alpha channel, so the color will not appear " +
                "visible.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ColorDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val InvalidColorHexValue = Issue.create(
            "InvalidColorHexValue",
            "Invalid Color hex value",
            "Creating a Color with a hex value requires a 32 bit value " +
                "(such as 0xFF000000), with 8 bits being used per channel (ARGB). Not passing a " +
                "full 32 bit value will result in channels being undefined / incorrect.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ColorDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
