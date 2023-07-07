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

package androidx.compose.animation.core.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isNotRemembered
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.util.isConstructorCall
import java.util.EnumSet

/**
 * [Detector] that checks `Animatable` calls to make sure that if they are called inside a
 * Composable body, they are `remember`ed.
 */
class UnrememberedAnimatableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolve() ?: return

            // Match calls to constructor, and top level 'factory' functions named Animatable
            // that return Animatable (this is needed as we have Animatable() factory functions
            // in separate modules for different types, and developers could create their own as
            // well).
            if (node.isConstructorCall()) {
                if (!method.isInPackageName(Names.AnimationCore.PackageName)) return
                if (method.containingClass?.name != Animatable.shortName) return
            } else {
                if (node.methodName != Animatable.shortName) return
                val returnType = method.returnType as? PsiClassReferenceType ?: return
                // Raw type since we want to ignore generic typing
                if (!returnType.rawType().equalsToText(Animatable.javaFqn)) return
            }

            if (node.isNotRemembered()) {
                context.report(
                    UnrememberedAnimatable,
                    node,
                    context.getNameLocation(node),
                    "Creating an Animatable during composition without using `remember`"
                )
            }
        }
    }

    companion object {
        val UnrememberedAnimatable = Issue.create(
            "UnrememberedAnimatable",
            "Creating an Animatable during composition without using `remember`",
            "Animatable instances created during composition need to be `remember`ed, " +
                "otherwise they will be recreated during recomposition, and lose their state. " +
                "Either hoist the Animatable to an object that is not created during composition," +
                " or wrap the Animatable in a call to `remember`.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                UnrememberedAnimatableDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val Animatable = Name(Names.AnimationCore.PackageName, "Animatable")
