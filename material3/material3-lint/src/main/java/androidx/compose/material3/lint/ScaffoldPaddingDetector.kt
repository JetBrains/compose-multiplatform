/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.lint

import androidx.compose.lint.Name
import androidx.compose.lint.findUnreferencedParameters
import androidx.compose.lint.isInPackageName
import androidx.compose.material3.lint.Material3Names.Material3
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.computeKotlinArgumentMapping
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression

/**
 * [Detector] that checks `Scaffold` usages for correctness.
 *
 * Scaffold provides an padding parameter to the `content` lambda. If this value is unused,
 * then the content may be obscured by app bars defined by the scaffold.
 */
class ScaffoldPaddingDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(Scaffold.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Material3.PackageName)) {
            val contentArgument = computeKotlinArgumentMapping(node, method)
                .orEmpty()
                .filter { (_, parameter) ->
                    parameter.name == "content"
                }
                .keys
                .filterIsInstance<ULambdaExpression>()
                .firstOrNull() ?: return

            contentArgument.findUnreferencedParameters().forEach { unreferencedParameter ->
                val location = unreferencedParameter.parameter
                    ?.let { context.getLocation(it) }
                    ?: context.getLocation(contentArgument)
                val name = unreferencedParameter.name
                context.report(
                    UnusedMaterial3ScaffoldPaddingParameter,
                    node,
                    location,
                    "Content padding parameter $name is not used"
                )
            }
        }
    }

    companion object {
        val UnusedMaterial3ScaffoldPaddingParameter = Issue.create(
            "UnusedMaterial3ScaffoldPaddingParameter",
            "Scaffold content should use the padding provided as a lambda parameter",
            "The `content` lambda in Scaffold has a padding parameter " +
                "which will include any inner padding for the content due to app bars. If this " +
                "parameter is ignored, then content may be obscured by the app bars resulting in " +
                "visual issues or elements that can't be interacted with.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ScaffoldPaddingDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val Scaffold = Name(Material3.PackageName, "Scaffold")