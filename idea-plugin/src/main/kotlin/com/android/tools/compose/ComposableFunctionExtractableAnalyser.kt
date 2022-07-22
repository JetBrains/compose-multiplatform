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

import com.android.tools.idea.flags.StudioFlags
import com.android.tools.modules.*
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AdditionalExtractableAnalyser
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractableCodeDescriptor
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

/**
 * Adds [COMPOSABLE_FQ_NAME] annotation to a function when it's extracted from a function annotated with [COMPOSABLE_FQ_NAME]
 * or Composable context.
 */
class ComposableFunctionExtractableAnalyser : AdditionalExtractableAnalyser {
  /**
   * Returns @Composable annotation of type of given KtLambdaArgument if there is any otherwise returns null.
   *
   * Example: fun myFunction(context: @Composable () -> Unit)
   * If given [KtLambdaArgument] corresponds to context parameter function returns [AnnotationDescriptor] for @Composable.
   */
  private fun KtLambdaArgument.getComposableAnnotation(bindingContext: BindingContext): AnnotationDescriptor? {
    val callExpression = parent as KtCallExpression
    val resolvedCall = callExpression.getResolvedCall(bindingContext)
    val argument = (resolvedCall?.getArgumentMapping(this) as? ArgumentMatch)?.valueParameter ?: return null
    return argument.type.annotations.findAnnotation(ComposeFqNames.Composable)
  }

  override fun amendDescriptor(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor {
    if (!StudioFlags.COMPOSE_FUNCTION_EXTRACTION.get() ||
        descriptor.extractionData.commonParent.inComposeModule() != true) {
      return descriptor
    }

    val bindingContext = descriptor.extractionData.bindingContext ?: return descriptor
    val sourceFunction = descriptor.extractionData.targetSibling
    if (sourceFunction is KtAnnotated) {
      val composableAnnotation = sourceFunction.findAnnotation(ComposeFqNames.Composable)?.resolveToDescriptorIfAny()
      if (composableAnnotation != null) {
        return descriptor.copy(annotations = descriptor.annotations + composableAnnotation)
      }
    }
    val outsideLambda = descriptor.extractionData.commonParent.parentOfType<KtLambdaArgument>(true)
    val composableAnnotation = outsideLambda?.getComposableAnnotation(bindingContext) ?: return descriptor
    return descriptor.copy(annotations = descriptor.annotations + composableAnnotation)
  }
}