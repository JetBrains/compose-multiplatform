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

package androidx.compose.runtime.saveable.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.Package
import androidx.compose.lint.inheritsFrom
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
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * [Detector] that checks `rememberSaveable` calls to make sure that a `Saver` is not passed to
 * the vararg argument.
 */
class RememberSaveableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(RememberSaveable.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(RuntimeSaveablePackageName)) return

        val argumentMapping = context.evaluator.computeArgumentMapping(node, method)
        // Filter the arguments provided to those that correspond to the varargs parameter.
        val varargArguments = argumentMapping.toList().filter { (_, parameter) ->
            // TODO: https://youtrack.jetbrains.com/issue/KT-45700 parameter.isVarArgs
            //  returns false because only varargs parameters at the end of a function are
            //  considered varargs in PSI, since Java callers can not call them as a vararg
            //  parameter otherwise. This is true for both KtLightParameterImpl, and
            //  ClsParameterImpl, so if we wanted to actually find if the parameter was
            //  vararg for Kotlin callers we would instead need to look through the metadata on
            //  the class file, or the source KtParameter.
            //  Instead since they are just treated as an array type in PSI, just find the
            //  corresponding array parameter.
            parameter.type is PsiArrayType
        }.map {
            // We don't need the parameter anymore, so just return the argument
            it.first
        }

        // Ignore if there are no vararg arguments provided, and ignore if there are multiple (we
        // assume if that multiple are provided then the developer knows what they are doing)
        if (varargArguments.size != 1) return

        val argument = varargArguments.first()
        val argumentType = argument.getExpressionType()

        // Ignore if the expression isn't a `Saver`
        if (argumentType?.inheritsFrom(Saver) != true) return

        // If the type is a MutableState, there is a second overload with a differently
        // named parameter we should use instead
        val isMutableStateSaver = node.getExpressionType()
            ?.inheritsFrom(Names.Runtime.MutableState) == true

        // TODO: might be safer to try and find the other overload through PSI, and get
        //  the parameter name directly.
        val parameterName = if (isMutableStateSaver) {
            "stateSaver"
        } else {
            "saver"
        }

        val argumentText = argument.sourcePsi?.text

        context.report(
            RememberSaveableSaverParameter,
            node,
            context.getLocation(argument),
            "Passing `Saver` instance to vararg `inputs`",
            argumentText?.let {
                val replacement = "$parameterName = $argumentText"
                LintFix.create()
                    .replace()
                    .name("Change to `$replacement`")
                    .text(argumentText)
                    .with(replacement)
                    .autoFix()
                    .build()
            }
        )
    }

    companion object {
        val RememberSaveableSaverParameter = Issue.create(
            "RememberSaveableSaverParameter",
            "`Saver` objects should be passed to the saver parameter, not the vararg " +
                "`inputs` parameter",
            "The first parameter to `rememberSaveable` is a vararg parameter for inputs that when" +
                " changed will cause the state to reset. Passing a `Saver` object to this " +
                "parameter is an error, as the intention is to pass the `Saver` object to the " +
                "saver parameter. Since the saver parameter is not the first parameter, it must " +
                "be explicitly named.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                RememberSaveableDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val RuntimeSaveablePackageName = Package("androidx.compose.runtime.saveable")
private val RememberSaveable = Name(RuntimeSaveablePackageName, "rememberSaveable")
private val Saver = Name(RuntimeSaveablePackageName, "Saver")
