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

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UVariable
import java.util.EnumSet

/**
 * [Detector] that checks the naming of CompositionLocal properties for consistency with guidelines.
 *
 * CompositionLocal properties should be prefixed with `Local` to make it clear that their value
 * is local to the current composition.
 */
class CompositionLocalNamingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UVariable::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitVariable(node: UVariable) {
            // Ignore parameters of type CompositionLocal
            if (node is UParameter) return
            if (node.sourcePsi is KtParameter) return
            // Ignore local properties
            if ((node.sourcePsi as? KtProperty)?.isLocal == true) return

            val type = node.type
            if (!type.inheritsFrom(Names.Runtime.CompositionLocal)) return

            val name = node.name
            if (name!!.startsWith("Local", ignoreCase = true)) return

            // Kotlinc can't disambiguate overloads for report / getNameLocation otherwise
            val uElementNode: UElement = node

            context.report(
                CompositionLocalNaming,
                uElementNode,
                context.getNameLocation(uElementNode),
                "CompositionLocal properties should be prefixed with `Local`",
            )
        }
    }

    companion object {
        val CompositionLocalNaming = Issue.create(
            "CompositionLocalNaming",
            "CompositionLocal properties should be prefixed with `Local`",
            "CompositionLocal properties should be prefixed with `Local`. This helps make " +
                "it clear at their use site that these values are local to the current " +
                "composition. Typically the full name will be `Local` + the type of the " +
                "CompositionLocal, for example val LocalFoo = compositionLocalOf { Foo() }.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                CompositionLocalNamingDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
