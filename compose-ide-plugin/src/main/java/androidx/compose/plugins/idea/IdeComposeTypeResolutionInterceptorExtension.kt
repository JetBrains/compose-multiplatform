/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.idea

import androidx.compose.plugins.kotlin.ComposeTypeResolutionInterceptorExtension
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

class IdeComposeTypeResolutionInterceptorExtension : ComposeTypeResolutionInterceptorExtension() {
    override fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        if (isComposeEnabled(expression)) {
            return super.interceptFunctionLiteralDescriptor(expression, context, descriptor)
        }
        return descriptor
    }

    override fun interceptType(
        element: KtElement,
        context: ExpressionTypingContext,
        resultType: KotlinType
    ): KotlinType {
        if (isComposeEnabled(element)) {
            return super.interceptType(element, context, resultType)
        }
        return resultType
    }
}