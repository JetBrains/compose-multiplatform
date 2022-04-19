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
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isNotRemembered
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
 * [Detector] that checks `mutableStateOf`, `mutableStateListOf`, and `mutableStateMapOf` calls to
 * make sure that if they are called inside a Composable body, they are `remember`ed.
 */
class UnrememberedMutableStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        Names.Runtime.MutableStateOf.shortName,
        Names.Runtime.MutableStateListOf.shortName,
        Names.Runtime.MutableStateMapOf.shortName,
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return

        if (node.isNotRemembered()) {
            context.report(
                UnrememberedMutableState,
                node,
                context.getNameLocation(node),
                "Creating a state object during composition without using `remember`"
            )
        }
    }

    companion object {
        val UnrememberedMutableState = Issue.create(
            "UnrememberedMutableState",
            "Creating a state object during composition without using `remember`",
            "State objects created during composition need to be `remember`ed, otherwise " +
                "they will be recreated during recomposition, and lose their state. Either hoist " +
                "the state to an object that is not created during composition, or wrap the " +
                "state in a call to `remember`.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                UnrememberedMutableStateDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
