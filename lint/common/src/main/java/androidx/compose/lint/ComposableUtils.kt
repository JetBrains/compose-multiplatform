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

package androidx.compose.lint

import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.impl.compiled.ClsParameterImpl
import kotlinx.metadata.jvm.annotations
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getContainingDeclaration
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.withContainingElements

/**
 * Returns whether this [UCallExpression] is invoked within the body of a Composable function or
 * lambda.
 *
 * This searches parent declarations until we find a lambda expression or a function, and looks
 * to see if these are Composable. Additionally, if we are inside a non-Composable lambda, the
 * lambda is a parameter on an inline function, and the inline function is within a Composable
 * lambda / function, this will also return true - since scoping functions / iterator functions
 * are commonly used within Composables.
 */
fun UCallExpression.isInvokedWithinComposable(): Boolean {
    // The nearest property / function / etc declaration that contains this call expression
    val containingDeclaration = getContainingDeclaration()

    // Look through containing elements until we find a lambda or a method
    for (element in withContainingElements) {
        when (element) {
            is ULambdaExpression -> {
                if (element.isComposable) {
                    return true
                }
                val parent = element.uastParent
                if (parent is KotlinUFunctionCallExpression && parent.isDeclarationInline) {
                    // We are now in a non-composable lambda parameter inside an inline function
                    // For example, a scoping function such as run {} or apply {} - since the
                    // body will be inlined and this is a common case, try to see if there is
                    // a parent composable function above us, since it is still most likely
                    // an error to call these methods inside an inline function, inside a
                    // Composable function.
                    continue
                } else {
                    return false
                }
            }
            is UMethod -> {
                return element.isComposable
            }
            // Stop when we reach the parent declaration to avoid escaping the scope. This
            // shouldn't be called unless there is a UAST type we don't handle above.
            containingDeclaration -> return false
        }
    }
    return false
}

// TODO: https://youtrack.jetbrains.com/issue/KT-45406
// KotlinUMethodWithFakeLightDelegate.hasAnnotation() (for reified functions for example)
// doesn't find annotations, so just look at the annotations directly.
// Note: annotations is deprecated but the replacement uAnnotations isn't available on the
// version of lint / uast we compile against, shouldn't be an issue when the above issue is fixed.
/**
 * Returns whether this method is @Composable or not
 */
@Suppress("DEPRECATION")
val UMethod.isComposable
    get() = annotations.any { it.qualifiedName == Names.Runtime.Composable.javaFqn }

/**
 * Returns whether this variable's type is @Composable or not
 */
val UVariable.isComposable: Boolean
    get() {
        // Annotation on the lambda
        val annotationOnLambda = when (val initializer = uastInitializer) {
            is ULambdaExpression -> {
                val source = initializer.sourcePsi
                if (source is KtFunction) {
                    // Anonymous function, val foo = @Composable fun() {}
                    source.hasComposableAnnotation
                } else {
                    // Lambda, val foo = @Composable {}
                    initializer.findAnnotation(Names.Runtime.Composable.javaFqn) != null
                }
            }
            else -> false
        }
        // Annotation on the type, foo: @Composable () -> Unit = { }
        val annotationOnType = typeReference?.isComposable == true
        return annotationOnLambda || annotationOnType
    }

/**
 * Returns whether this parameter's type is @Composable or not
 */
val UParameter.isComposable: Boolean
    get() = when (sourcePsi) {
        // The parameter is in a class file. Currently type annotations aren't currently added to
        // the underlying type (https://youtrack.jetbrains.com/issue/KT-45307), so instead we use
        // the metadata annotation.
        is ClsParameterImpl -> {
            // Find the containing method, so we can get metadata from the containing class
            val containingMethod = getContainingUMethod()!!.sourcePsi as ClsMethodImpl
            val kmFunction = containingMethod.toKmFunction()

            val kmValueParameter = kmFunction?.valueParameters?.find {
                it.name == name
            }

            kmValueParameter?.type?.annotations?.find {
                it.className == Names.Runtime.Composable.kmClassName
            } != null
        }
        // The parameter is in a source declaration
        else -> typeReference!!.isComposable
    }

/**
 * Returns whether this lambda expression is @Composable or not
 */
val ULambdaExpression.isComposable: Boolean
    get() = when (val lambdaParent = uastParent) {
        // Function call with a lambda parameter
        is UCallExpression -> {
            val parameter = lambdaParent.getParameterForArgument(this)
            (parameter.toUElement() as? UParameter)?.isComposable == true
        }
        // A local / non-local lambda variable
        is UVariable -> {
            lambdaParent.isComposable
        }
        // Either a new UAST type we haven't handled, or non-Kotlin declarations
        else -> false
    }

/**
 * Returns whether this type reference is @Composable or not
 */
private val UTypeReferenceExpression.isComposable: Boolean
    get() {
        if (type.hasAnnotation(Names.Runtime.Composable.javaFqn)) return true

        // Annotations should be available on the PsiType itself in 1.4.30+, but we are
        // currently on an older version of UAST / Kotlin embedded compiled
        // (https://youtrack.jetbrains.com/issue/KT-45244), so we need to manually check the
        // underlying type reference. Until then, the above check will always fail.
        return (sourcePsi as? KtTypeReference)?.hasComposableAnnotation == true
    }

/**
 * Returns whether this annotated declaration has a Composable annotation
 */
private val KtAnnotated.hasComposableAnnotation: Boolean
    get() = annotationEntries.any {
        (it.toUElement() as UAnnotation).qualifiedName == Names.Runtime.Composable.javaFqn
    }
