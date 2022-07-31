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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getValueArgumentForExpression
import org.jetbrains.kotlin.util.OperatorNameConventions


internal fun ResolvedCall<*>.isComposableInvocation(): Boolean {
  if (this is VariableAsFunctionResolvedCall) {
    if (variableCall.candidateDescriptor.type.hasComposableAnnotation())
      return true
    if (functionCall.resultingDescriptor.hasComposableAnnotation()) return true
    return false
  }
  val candidateDescriptor = candidateDescriptor
  if (candidateDescriptor is FunctionDescriptor) {
    if (candidateDescriptor.isOperator &&
        candidateDescriptor.name == OperatorNameConventions.INVOKE
    ) {
      if (dispatchReceiver?.type?.hasComposableAnnotation() == true) {
        return true
      }
    }
  }
  return when (candidateDescriptor) {
    is ValueParameterDescriptor -> false
    is LocalVariableDescriptor -> false
    is PropertyDescriptor -> {
      val isGetter = valueArguments.isEmpty()
      val getter = candidateDescriptor.getter
      if (isGetter && getter != null) {
        getter.hasComposableAnnotation()
      } else {
        false
      }
    }
    is PropertyGetterDescriptor ->
      candidateDescriptor.correspondingProperty.hasComposableAnnotation()
    else -> candidateDescriptor.hasComposableAnnotation()
  }
}

internal fun getArgumentDescriptor(
  argument: KtFunction,
  bindingContext: BindingContext
): ValueParameterDescriptor? {
  val call = KtPsiUtil.getParentCallIfPresent(argument) ?: return null
  val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
  val valueArgument = resolvedCall.call.getValueArgumentForExpression(argument) ?: return null
  val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
  return mapping.valueParameter
}

internal fun List<KtAnnotationEntry>.hasComposableAnnotation(bindingContext: BindingContext): Boolean {
  for (entry in this) {
    val descriptor = bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
    if (descriptor.isComposableAnnotation) return true
  }
  return false
}