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
package com.android.tools.compose.code.completion

import com.android.tools.compose.COMPOSE_MODIFIER_FQN
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.modules.*
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.contextOfType
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.util.getResolveScope
import org.jetbrains.kotlin.idea.completion.BasicLookupElementFactory
import org.jetbrains.kotlin.idea.completion.CollectRequiredTypesContextVariablesProvider
import org.jetbrains.kotlin.idea.completion.CompletionSession
import org.jetbrains.kotlin.idea.completion.InsertHandlerProvider
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.receiverTypesWithIndex
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/**
 * Enhances code completion for Modifier (androidx.compose.ui.Modifier)
 *
 * Adds Modifier extension functions to code completion in places where modifier is expected
 * e.g. parameter of type Modifier, variable of type Modifier as it was called on Modifier.<caret>
 *
 * Moves extension functions for method called on modifier [isMethodCalledOnModifier] up in the completion list.
 *
 * @see COMPOSE_MODIFIER_FQN
 */
class ComposeModifierCompletionContributor : CompletionContributor() {

  override fun fillCompletionVariants(parameters: CompletionParameters, resultSet: CompletionResultSet) {
    val element = parameters.position
    if (!StudioFlags.COMPOSE_EDITOR_SUPPORT.get() || !element.inComposeModule() || parameters.originalFile !is KtFile) {
      return
    }

    // It says "on imported" because only in that case we are able resolve that it called on Modifier.
    val isMethodCalledOnImportedModifier = element.isMethodCalledOnModifier()
    ProgressManager.checkCanceled()
    val isModifierType = isMethodCalledOnImportedModifier || element.isModifierArgument || element.isModifierProperty
    if (!isModifierType) return

    ProgressManager.checkCanceled()

    val nameExpression = createNameExpression(element)

    val extensionFunctions = getExtensionFunctionsForModifier(nameExpression, element, resultSet.prefixMatcher)

    ProgressManager.checkCanceled()
    val (returnsModifier, others) = extensionFunctions.partition { it.returnType?.fqName?.asString() == COMPOSE_MODIFIER_FQN }
    val lookupElementFactory = createLookupElementFactory(nameExpression, parameters)

    val isNewModifier = !isMethodCalledOnImportedModifier && element.parentOfType<KtDotQualifiedExpression>() == null
    //Prioritise functions that return Modifier over other extension function.
    resultSet.addAllElements(returnsModifier.toLookupElements(lookupElementFactory, 2.0, insertModifier = isNewModifier))
    //If user didn't type Modifier don't suggest extensions that doesn't return Modifier.
    if (isMethodCalledOnImportedModifier) {
      resultSet.addAllElements(others.toLookupElements(lookupElementFactory, 0.0, insertModifier = isNewModifier))
    }

    ProgressManager.checkCanceled()

    //If method is called on modifier [KotlinCompletionContributor] will add extensions function one more time, we need to filter them out.
    if (isMethodCalledOnImportedModifier) {
      val extensionFunctionsNames = extensionFunctions.map { it.name.asString() }.toSet()
      resultSet.runRemainingContributors(parameters) { completionResult ->
        val skipResult = completionResult.lookupElement.psiElement.safeAs<KtFunction>()?.name?.let { extensionFunctionsNames.contains(it) }
        if (skipResult != true) {
          resultSet.passResult(completionResult)
        }
      }
    }
  }

  private fun List<CallableDescriptor>.toLookupElements(
    lookupElementFactory: LookupElementFactory,
    weight: Double,
    insertModifier: Boolean
  ) = flatMap { descriptor ->
    lookupElementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true).map {
      PrioritizedLookupElement.withPriority(ModifierLookupElement(it, insertModifier), weight)
    }
  }

  /**
   * Creates LookupElementFactory that is similar to the one kotlin-plugin uses during completion session.
   * Code partially copied from [CompletionSession].
   */
  private fun createLookupElementFactory(nameExpression: KtSimpleNameExpression, parameters: CompletionParameters): LookupElementFactory {
    val bindingContext = nameExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
    val file = parameters.originalFile.safeAs<KtFile>()!!
    val resolutionFacade = file.getResolutionFacade()

    val moduleDescriptor = resolutionFacade.moduleDescriptor

    val callTypeAndReceiver = CallTypeAndReceiver.detect(nameExpression)
    val receiverTypes = callTypeAndReceiver.receiverTypesWithIndex(
      bindingContext, nameExpression, moduleDescriptor, resolutionFacade,
      stableSmartCastsOnly = true, /* we don't include smart cast receiver types for "unstable" receiver value to mark members grayed */
      withImplicitReceiversWhenExplicitPresent = true
    )

    val inDescriptor = nameExpression.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor

    val insertHandler = InsertHandlerProvider(CallType.DOT, parameters.editor, ::emptyList)
    val basicLookupElementFactory = BasicLookupElementFactory(nameExpression.project, insertHandler)

    return LookupElementFactory(
      basicLookupElementFactory, parameters.editor, receiverTypes,
      callTypeAndReceiver.callType, inDescriptor, CollectRequiredTypesContextVariablesProvider()
    )
  }

  /**
   * Creates "Modifier.call" expression as it would be if user typed "Modifier.<caret>" themselves.
   */
  private fun createNameExpression(originalElement: PsiElement): KtSimpleNameExpression {
    val originalFile = originalElement.containingFile.safeAs<KtFile>()!!

    val file = KtPsiFactory(originalFile.project).createAnalyzableFile("temp.kt", "val x = $COMPOSE_MODIFIER_FQN.call", originalFile)
    return file.getChildOfType<KtProperty>()!!.getChildOfType<KtDotQualifiedExpression>()!!.lastChild as KtSimpleNameExpression
  }

  private fun getExtensionFunctionsForModifier(
    nameExpression: KtSimpleNameExpression,
    originalPosition: PsiElement,
    prefixMatcher: PrefixMatcher
  ): Collection<CallableDescriptor> {
    val file = nameExpression.containingFile as KtFile
    val searchScope = getResolveScope(file)
    val resolutionFacade = file.getResolutionFacade()
    val bindingContext = nameExpression.analyze(BodyResolveMode.PARTIAL_FOR_COMPLETION)

    val callTypeAndReceiver = CallTypeAndReceiver.detect(nameExpression)
    fun isVisible(descriptor: DeclarationDescriptor): Boolean {
      if (descriptor is DeclarationDescriptorWithVisibility) {
        return descriptor.isVisible(originalPosition, callTypeAndReceiver.receiver as? KtExpression, bindingContext, resolutionFacade)
      }

      return true
    }

    val indicesHelper = KotlinIndicesHelper(resolutionFacade, searchScope, ::isVisible, file = file)

    val nameFilter = { name: String -> prefixMatcher.prefixMatches(name) }
    return indicesHelper.getCallableTopLevelExtensions(callTypeAndReceiver, nameExpression, bindingContext, null, nameFilter)
  }

  private val PsiElement.isModifierProperty: Boolean
    get() {
      // Case val myModifier:Modifier = <caret>
      val property = parent?.parent?.safeAs<KtProperty>() ?: return false
      return property.type()?.fqName?.asString() == COMPOSE_MODIFIER_FQN
    }

  private val PsiElement.isModifierArgument: Boolean
    get() {
      val argument = contextOfType<KtValueArgument>().takeIf { it !is KtLambdaArgument } ?: return false

      val callExpression = argument.parentOfType<KtCallElement>() ?: return false
      val callee = callExpression.calleeExpression?.mainReference?.resolve().safeAs<KtNamedFunction>() ?: return false

      val argumentTypeFqName = if (argument.isNamed()) {
        val argumentName = argument.getArgumentName()!!.asName.asString()
        callee.valueParameters.find { it.name == argumentName }?.type()?.fqName
      }
      else {
        val argumentIndex = (argument.parent as KtValueArgumentList).arguments.indexOf(argument)
        callee.valueParameters.getOrNull(argumentIndex)?.type()?.fqName
      }

      return argumentTypeFqName?.asString() == COMPOSE_MODIFIER_FQN
    }

  /**
   * Returns true if psiElement is method called on object that has Modifier type.
   *
   * Returns true for Modifier.align().%this%, myModifier.%this%, Modifier.%this%.
   */
  private fun PsiElement.isMethodCalledOnModifier(): Boolean {
    val elementOnWhichMethodCalled: KtExpression = parent.safeAs<KtNameReferenceExpression>()?.getReceiverExpression() ?: return false
    // Case Modifier.align().%this%, modifier.%this%
    val fqName = elementOnWhichMethodCalled.resolveToCall(BodyResolveMode.PARTIAL)?.getReturnType()?.fqName ?:
                 // Case Modifier.%this%
                 elementOnWhichMethodCalled.safeAs<KtNameReferenceExpression>()?.resolve().safeAs<KtClass>()?.fqName
    return fqName?.asString() == COMPOSE_MODIFIER_FQN
  }

  /**
   * Inserts "Modifier." before [delegate] and imports [ComposeModifierCompletionContributor.modifierFqName] if it's not imported.
   */
  private class ModifierLookupElement(
    delegate: LookupElement,
    val insertModifier: Boolean
  ) : LookupElementDecorator<LookupElement>(delegate) {
    companion object {
      private const val callOnModifierObject = "Modifier."
    }

    override fun renderElement(presentation: LookupElementPresentation) {
      super.renderElement(presentation)
      presentation.itemText = lookupString
    }

    override fun getAllLookupStrings(): MutableSet<String> {
      if (insertModifier) {
        val lookupStrings = super.getAllLookupStrings().toMutableSet()
        lookupStrings.add(callOnModifierObject + super.getLookupString())
        return lookupStrings
      }
      return super.getAllLookupStrings()
    }

    override fun getLookupString(): String {
      if (insertModifier) {
        return callOnModifierObject + super.getLookupString()
      }
      return super.getLookupString()
    }

    override fun handleInsert(context: InsertionContext) {
      val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
      // Compose plugin inserts Modifier if completion character is '\n', doesn't happened with '\t'. Looks like a bug.
      if (insertModifier && context.completionChar != '\n') {
        context.document.insertString(context.startOffset, callOnModifierObject)
        context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, context.startOffset + callOnModifierObject.length)
        psiDocumentManager.commitAllDocuments()
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
      }
      val ktFile = context.file as KtFile
      val modifierDescriptor = ktFile.resolveImportReference(FqName(COMPOSE_MODIFIER_FQN)).singleOrNull()
      modifierDescriptor?.let { ImportInsertHelper.getInstance(context.project).importDescriptor(ktFile, it) }
      psiDocumentManager.commitAllDocuments()
      psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
      super.handleInsert(context)
    }
  }
}
