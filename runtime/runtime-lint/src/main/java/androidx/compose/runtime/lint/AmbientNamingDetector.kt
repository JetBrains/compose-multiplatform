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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UVariable
import java.util.EnumSet
import java.util.Locale

/**
 * [Detector] that checks the naming of Ambient properties for consistency with guidelines.
 *
 * - `Ambient` should not be used as a noun (suffix) in the name of an Ambient property. It may
 * be used as an adjective (prefix) in lieu of a more descriptive adjective.
 */
class AmbientNamingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UVariable::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitVariable(node: UVariable) {
            val type = node.type
            if (!InheritanceUtil.isInheritor(type, AmbientFqn)) return

            val name = node.name
            if (!name!!.endsWith(AmbientShortName)) return

            val newName = AmbientShortName + name.replace(AmbientShortName, "")
                .capitalize(Locale.getDefault())

            // Kotlinc can't disambiguate overloads for report / getNameLocation otherwise
            val uElementNode: UElement = node

            context.report(
                AmbientNaming,
                uElementNode,
                context.getNameLocation(uElementNode),
                "`Ambient` should not be used as a noun when naming Ambient properties",
                LintFix.create()
                    .replace()
                    .name("Use Ambient as an adjective (prefix)")
                    .text(name)
                    .with(newName)
                    .autoFix()
                    .build()
            )
        }
    }

    companion object {
        val AmbientNaming = Issue.create(
            "AmbientNaming",
            "Incorrect naming for Ambient properties",
            "`Ambient` should not be used as a anoun when naming Ambient properties. It may be " +
                "used as an adjective in lieu of a more descriptive adjective. Otherwise Ambients" +
                " follow standard property naming guidelines.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                AmbientNamingDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private const val AmbientFqn = "androidx.compose.runtime.Ambient"
private val AmbientShortName get() = AmbientFqn.split(".").last()
