/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tools.compose

import androidx.compose.compiler.plugins.kotlin.ComposeTypeResolutionInterceptorExtension
import com.android.tools.compose.ComposeWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR
import com.android.tools.idea.flags.StudioFlags
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getAnnotationEntries
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class ComposePluginTypeResolutionInterceptorExtension : ComposeTypeResolutionInterceptorExtension() {
  override fun interceptFunctionLiteralDescriptor(
    expression: KtLambdaExpression,
    context: ExpressionTypingContext,
    descriptor: AnonymousFunctionDescriptor
  ): AnonymousFunctionDescriptor {

    if (StudioFlags.COMPOSE_DEPLOY_LIVE_EDIT_USE_EMBEDDED_COMPILER.get()) {
      return super.interceptFunctionLiteralDescriptor(expression, context, descriptor)
    }

    if (descriptor.isSuspend) return descriptor
    if (
      context.expectedType.hasComposableAnnotation() &&
      !descriptor.hasComposableAnnotation()
    ) {
      // If the expected type has an @Composable annotation then the literal function
      // expression should infer a an @Composable annotation
      context.trace.record(INFERRED_COMPOSABLE_DESCRIPTOR, descriptor, true)
    }
    val arg = getArgumentDescriptor(expression.functionLiteral, context.trace.bindingContext)

    val argTypeDescriptor = arg
      ?.type
      ?.constructor
      ?.declarationDescriptor as? ClassDescriptor
    if (argTypeDescriptor != null) {
      val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
      if (sam != null && sam.hasComposableAnnotation()) {
        context.trace.record(INFERRED_COMPOSABLE_DESCRIPTOR, descriptor, true)
      }
    }
    return descriptor
  }

  override fun interceptType(
    element: KtElement,
    context: ExpressionTypingContext,
    resultType: KotlinType
  ): KotlinType {
    if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
    if (element !is KtLambdaExpression) return resultType

    val arg = getArgumentDescriptor(element.functionLiteral, context.trace.bindingContext)

    val argTypeDescriptor = arg
      ?.type
      ?.constructor
      ?.declarationDescriptor as? ClassDescriptor
    if (argTypeDescriptor != null) {
      val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
      if (sam != null && sam.hasComposableAnnotation()) {
        context.trace.record(
          ComposeWritableSlices.INFERRED_COMPOSABLE_LITERAL,
          element,
          true
        )
        return resultType.makeComposable(context.scope.ownerDescriptor.module)
      }
    }

    if (
      element.getAnnotationEntries().hasComposableAnnotation(context.trace.bindingContext) ||
      context.expectedType.hasComposableAnnotation()
    ) {
      context.trace.record(ComposeWritableSlices.INFERRED_COMPOSABLE_LITERAL, element, true)
      return resultType.makeComposable(context.scope.ownerDescriptor.module)
    }
    return resultType
  }
}