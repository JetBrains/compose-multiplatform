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
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInvokedWithinComposable
import androidx.compose.lint.toKmFunction
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.impl.compiled.ClsMethodImpl
import kotlinx.metadata.KmClassifier
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement
import java.util.EnumSet

/**
 * [Detector] that checks calls to Flow operator functions (such as map) to make sure they don't
 * happen inside the body of a composable function / lambda. This detector defines an operator
 * function as any function with a receiver of Flow, and a return type of Flow, such as:
 *
 * fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R>
 * fun <T> Flow<T>.drop(count: Int): Flow<T>
 */
class ComposableFlowOperatorDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val method = node.resolveToUElement() as? UMethod ?: return
            val receiverType = node.receiverType

            // We are calling a method on a `Flow` type, and the method is an operator function
            if (receiverType?.inheritsFrom(FlowName) == true && method.isFlowOperator()) {
                if (node.isInvokedWithinComposable()) {
                    context.report(
                        FlowOperatorInvokedInComposition,
                        node,
                        context.getNameLocation(node),
                        "Flow operator functions should not be invoked within composition"
                    )
                }
            }
        }
    }

    companion object {
        val FlowOperatorInvokedInComposition = Issue.create(
            "FlowOperatorInvokedInComposition",
            "Flow operator functions should not be invoked within composition",
            "Calling a Flow operator function within composition will result in a new " +
                "Flow being created every recomposition, which will reset collectAsState() and " +
                "cause other related problems. Instead Flow operators should be called inside " +
                "`remember`, or a side effect such as LaunchedEffect.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ComposableFlowOperatorDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

/**
 * @return whether this [UMethod] is an extension function with a receiver of Flow, and a
 * return type of Flow
 */
private fun UMethod.isFlowOperator(): Boolean {
    // Whether this method returns Flow
    if (returnType?.inheritsFrom(FlowName) != true) {
        return false
    }
    // Whether this method is an extension on Flow
    return when (val source = sourcePsi) {
        // Parsing a class file
        is ClsMethodImpl -> {
            val kmFunction = source.toKmFunction()
            kmFunction?.receiverParameterType?.classifier == FlowClassifier
        }
        // Parsing Kotlin source
        is KtNamedFunction -> {
            val receiver = source.receiverTypeReference
            (receiver.toUElement() as? UTypeReferenceExpression)
                ?.getQualifiedName() == FlowName.javaFqn
        }
        // Should never happen, safe return if it does
        else -> false
    }
}

private val FlowPackageName = Package("kotlinx.coroutines.flow")
private val FlowName = Name(FlowPackageName, "Flow")
private val FlowClassifier = KmClassifier.Class(FlowName.kmClassName)