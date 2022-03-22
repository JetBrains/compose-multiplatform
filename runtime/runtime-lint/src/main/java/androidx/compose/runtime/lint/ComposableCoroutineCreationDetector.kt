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

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isInvokedWithinComposable
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * [Detector] that checks `async` and `launch` calls to make sure they don't happen inside the
 * body of a composable function / lambda.
 */
class ComposableCoroutineCreationDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf(
        Async.shortName,
        Launch.shortName,
        LaunchIn.shortName,
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!(method.isInPackageName(CoroutinePackageName) ||
                method.isInPackageName(FlowPackageName))
        ) return

        if (node.isInvokedWithinComposable()) {
            context.report(
                CoroutineCreationDuringComposition,
                node,
                context.getNameLocation(node),
                "Calls to ${method.name} should happen inside a LaunchedEffect and " +
                    "not composition"
            )
        }
    }

    companion object {
        val CoroutineCreationDuringComposition = Issue.create(
            "CoroutineCreationDuringComposition",
            "Calls to `async` or `launch` should happen inside a LaunchedEffect and not " +
                "composition",
            "Creating a coroutine with `async` or `launch` during composition is often incorrect " +
                "- this means that a coroutine will be created even if the composition fails / is" +
                " rolled back, and it also means that multiple coroutines could end up mutating " +
                "the same state, causing inconsistent results. Instead, use `LaunchedEffect` and " +
                "create coroutines inside the suspending block. The block will only run after a " +
                "successful composition, and will cancel existing coroutines when `key` changes, " +
                "allowing correct cleanup.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ComposableCoroutineCreationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val CoroutinePackageName = Package("kotlinx.coroutines")
private val FlowPackageName = Package("kotlinx.coroutines.flow")
private val Async = Name(CoroutinePackageName, "async")
private val Launch = Name(CoroutinePackageName, "launch")
private val LaunchIn = Name(FlowPackageName, "launchIn")
