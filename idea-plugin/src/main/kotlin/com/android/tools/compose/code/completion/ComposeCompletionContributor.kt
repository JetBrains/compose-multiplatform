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

import com.android.tools.compose.COMPOSABLE_FQ_NAMES
import com.android.tools.compose.ComposeSettings
import com.android.tools.compose.isComposableFunction
import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.flags.StudioFlags.COMPOSE_COMPLETION_INSERT_HANDLER
import com.android.tools.idea.flags.StudioFlags.COMPOSE_COMPLETION_PRESENTATION
import com.android.tools.modules.*
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.daemon.impl.quickfix.EmptyExpression
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.castSafelyTo
import icons.StudioIcons
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.completion.BasicLookupElementFactory
import org.jetbrains.kotlin.idea.completion.LambdaSignatureTemplates
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.completion.handlers.KotlinCallableInsertHandler
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.results.argumentValueType
import org.jetbrains.kotlin.types.typeUtil.isUnit

private val COMPOSABLE_FUNCTION_ICON = StudioIcons.Compose.Editor.COMPOSABLE_FUNCTION

/**
 * Checks if this completion is for a statement (where Compose views usually called) and not part of another expression.
 */
private fun CompletionParameters.isForStatement(): Boolean {
  return position is LeafPsiElement &&
         position.node.elementType == KtTokens.IDENTIFIER &&
         position.parent?.parent is KtBlockExpression
}

private fun LookupElement.getFunctionDescriptor(): FunctionDescriptor? {
  return this.`object`
    .castSafelyTo<DeclarationLookupObject>()
    ?.descriptor
    ?.castSafelyTo<FunctionDescriptor>()
}

private val List<ValueParameterDescriptor>.hasComposableChildren: Boolean
  get() {
    val lastArgType = lastOrNull()?.type ?: return false
    return lastArgType.isBuiltinFunctionalType
           && COMPOSABLE_FQ_NAMES.any { lastArgType.annotations.hasAnnotation(FqName(it)) }
  }

private val ValueParameterDescriptor.isLambdaWithNoParameters: Boolean
  get() = type.isFunctionType
          // The only type in the list is the return type (can be Unit).
          && argumentValueType.arguments.size == 1

/**
 * true if the last parameter is required, and a lambda type with no parameters.
 */
private val List<ValueParameterDescriptor>.isLastRequiredLambdaWithNoParameters: Boolean
  get() {
    val lastParameter = lastOrNull() ?: return false
    return !lastParameter.hasDefaultValue() && lastParameter.isLambdaWithNoParameters
  }


/**
 * Find the [CallType] from the [InsertionContext]. The [CallType] can be used to detect if the completion is being done in a regular
 * statement, an import or some other expression and decide if we want to apply the [ComposeInsertHandler].
 */
private fun InsertionContext.inferCallType(): CallType<*> {
  // Look for an existing KtSimpleNameExpression to pass to CallTypeAndReceiver.detect so we can infer the call type.
  val namedExpression = (file.findElementAt(startOffset)?.parent as? KtSimpleNameExpression)?.mainReference?.expression
                        ?: return CallType.DEFAULT

  return CallTypeAndReceiver.detect(namedExpression).callType
}

/**
 * Modifies [LookupElement]s for composable functions, to improve Compose editing UX.
 */
class ComposeCompletionContributor : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, resultSet: CompletionResultSet) {
    if (!StudioFlags.COMPOSE_EDITOR_SUPPORT.get() ||
        parameters.position.inComposeModule() != true ||
        parameters.position.language != KotlinLanguage.INSTANCE) {
      return
    }

    resultSet.runRemainingContributors(parameters) { completionResult ->
      val lookupElement = completionResult.lookupElement
      val psi = lookupElement.psiElement
      val newResult = when {
        psi == null || !psi.isComposableFunction() -> completionResult
        lookupElement.isForSpecialLambdaLookupElement() -> null
        else -> completionResult.withLookupElement(ComposeLookupElement(lookupElement))
      }

      newResult?.let(resultSet::passResult)
    }
  }

  /**
   * Checks if the [LookupElement] is an additional, "special" lookup element created for functions that can be invoked using the lambda
   * syntax. These are created by [LookupElementFactory.addSpecialFunctionCallElements] and can be confusing for Compose APIs that often
   * use overloaded function names.
   */
  private fun LookupElement.isForSpecialLambdaLookupElement(): Boolean {
    val presentation = LookupElementPresentation()
    renderElement(presentation)
    return presentation.tailText?.startsWith(" {...} (..., ") ?: false
  }
}

/**
 * Wraps original Kotlin [LookupElement]s for composable functions to make them stand out more.
 */
private class ComposeLookupElement(original: LookupElement) : LookupElementDecorator<LookupElement>(original) {
  /**
   * Set of [CallType]s that should be handled by the [ComposeInsertHandler].
   */
  private val validCallTypes = setOf(CallType.DEFAULT, CallType.DOT)

  init {
    require(original.psiElement?.isComposableFunction() == true)
  }

  override fun getPsiElement(): KtNamedFunction = super.getPsiElement() as KtNamedFunction

  override fun renderElement(presentation: LookupElementPresentation) {
    super.renderElement(presentation)

    if (COMPOSE_COMPLETION_PRESENTATION.get()) {
      val descriptor = getFunctionDescriptor() ?: return
      presentation.icon = COMPOSABLE_FUNCTION_ICON
      presentation.setTypeText(if (descriptor.returnType?.isUnit() == true) null else presentation.typeText, null)
      rewriteSignature(descriptor, presentation)
    }
  }

  override fun handleInsert(context: InsertionContext) {
    val descriptor = getFunctionDescriptor()
    val callType by lazy { context.inferCallType() }
    return when {
      !COMPOSE_COMPLETION_INSERT_HANDLER.get() -> super.handleInsert(context)
      !ComposeSettings.getInstance().state.isComposeInsertHandlerEnabled -> super.handleInsert(context)
      descriptor == null -> super.handleInsert(context)
      !validCallTypes.contains(callType) -> super.handleInsert(context)
      else -> ComposeInsertHandler(descriptor, callType).handleInsert(context, this)
    }
  }

  private fun rewriteSignature(descriptor: FunctionDescriptor, presentation: LookupElementPresentation) {
    val allParameters = descriptor.valueParameters
    val requiredParameters = allParameters.filter { !it.declaresDefaultValue() }
    val inParens = if (requiredParameters.hasComposableChildren) requiredParameters.dropLast(1) else requiredParameters
    val renderer = when {
      requiredParameters.size < allParameters.size -> SHORT_NAMES_WITH_DOTS
      inParens.isEmpty() && requiredParameters.hasComposableChildren -> {
        // Don't render an empty pair of parenthesis if we're rendering a lambda afterwards.
        null
      }
      else -> BasicLookupElementFactory.SHORT_NAMES_RENDERER
    }

    presentation.clearTail()
    renderer
      ?.renderValueParameters(inParens, false)
      ?.let { presentation.appendTailTextItalic(it, false) }

    if (requiredParameters.hasComposableChildren) {
      presentation.appendTailText(" " + LambdaSignatureTemplates.DEFAULT_LAMBDA_PRESENTATION, true)
    }
  }
}

/**
 * A version of [BasicLookupElementFactory.SHORT_NAMES_RENDERER] that adds `, ...)` at the end of the parameters list.
 */
private val SHORT_NAMES_WITH_DOTS = BasicLookupElementFactory.SHORT_NAMES_RENDERER.withOptions {
  val delegate = DescriptorRenderer.ValueParametersHandler.DEFAULT
  valueParametersHandler = object : DescriptorRenderer.ValueParametersHandler {
    override fun appendAfterValueParameter(
      parameter: ValueParameterDescriptor,
      parameterIndex: Int,
      parameterCount: Int,
      builder: StringBuilder
    ) {
      delegate.appendAfterValueParameter(parameter, parameterIndex, parameterCount, builder)
    }

    override fun appendBeforeValueParameter(
      parameter: ValueParameterDescriptor,
      parameterIndex: Int,
      parameterCount: Int,
      builder: StringBuilder
    ) {
      delegate.appendBeforeValueParameter(parameter, parameterIndex, parameterCount, builder)
    }

    override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
      delegate.appendBeforeValueParameters(parameterCount, builder)
    }

    override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
      builder.append(if (parameterCount == 0) "...)" else ", ...)")
    }
  }
}

/**
 * Set of Composable FQNs that have a conflicting name with a non-composable and where we want to promote the
 * non-composable instead.
 */
private val COMPOSABLE_CONFLICTING_NAMES = setOf(
  "androidx.compose.material.MaterialTheme"
)

/**
 * Custom [CompletionWeigher] which moves composable functions up the completion list.
 *
 * It doesn't give composable functions "absolute" priority, some weighers are hardcoded to run first: specifically one that puts prefix
 * matches above [LookupElement]s where the match is in the middle of the name. Overriding this behavior would require an extension point in
 * [org.jetbrains.kotlin.idea.completion.CompletionSession.createSorter].
 *
 * See [com.intellij.codeInsight.completion.PrioritizedLookupElement] for more information on how ordering of lookup elements works and how
 * to debug it.
 */
class ComposeCompletionWeigher : CompletionWeigher() {
  override fun weigh(element: LookupElement, location: CompletionLocation): Int = when {
      !StudioFlags.COMPOSE_EDITOR_SUPPORT.get() -> 0
      !StudioFlags.COMPOSE_COMPLETION_WEIGHER.get() -> 0
      location.completionParameters.position.language != KotlinLanguage.INSTANCE -> 0
      location.completionParameters.position.inComposeModule() != true -> 0
      element.isForNamedArgument() -> 3
      location.completionParameters.isForStatement() -> {
        val isConflictingName = COMPOSABLE_CONFLICTING_NAMES.contains((element.psiElement as? KtNamedDeclaration)?.fqName?.asString() ?: "")
        val isComposableFunction = element.psiElement?.isComposableFunction() ?: false
        // This method ensures that the order of completion ends up as:
        //
        // Composables with non-conflicting names (like Button {}) +2
        // Non Composables with conflicting names (like the MaterialTheme object) +2
        // Composable with conflicting names      (like MaterialTheme {}) +1
        // Anything else 0
        when {
          isComposableFunction && !isConflictingName -> 2
          !isComposableFunction && isConflictingName -> 2
          isComposableFunction && isConflictingName -> 1
          else -> 0
        }
      }
      else -> 0
    }

  private fun LookupElement.isForNamedArgument() = lookupString.endsWith(" =")
}

private fun InsertionContext.getNextElementIgnoringWhitespace(): PsiElement? {
  val elementAtCaret = file.findElementAt(editor.caretModel.offset) ?: return null
  return elementAtCaret.getNextSiblingIgnoringWhitespace(true)
}

private fun InsertionContext.isNextElementOpenCurlyBrace() = getNextElementIgnoringWhitespace()?.text?.startsWith("{") == true

private fun InsertionContext.isNextElementOpenParenthesis() = getNextElementIgnoringWhitespace()?.text?.startsWith("(") == true

private class ComposeInsertHandler(
  private val descriptor: FunctionDescriptor,
  callType: CallType<*>) : KotlinCallableInsertHandler(callType) {
  override fun handleInsert(context: InsertionContext, item: LookupElement) = with(context) {
    super.handleInsert(context, item)

    if (isNextElementOpenParenthesis()) return

    // All Kotlin insertion handlers do this, possibly to post-process adding a new import in the call to super above.
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.commitAllDocuments()
    psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

    val templateManager = TemplateManager.getInstance(project)
    val allParameters = descriptor.valueParameters
    val requiredParameters = allParameters.filter { !it.declaresDefaultValue() }
    val insertLambda = requiredParameters.hasComposableChildren
                       || allParameters.isLastRequiredLambdaWithNoParameters
    val inParens = if (insertLambda) requiredParameters.dropLast(1) else requiredParameters

    val template = templateManager.createTemplate("", "").apply {
      isToReformat = true
      setToIndent(true)

      when {
        inParens.isNotEmpty() -> {
          addTextSegment("(")
          inParens.forEachIndexed { index, parameter ->
            if (index > 0) {
              addTextSegment(", ")
            }
            addTextSegment(parameter.name.asString() + " = ")
            if (parameter.isLambdaWithNoParameters) {
              addVariable(ConstantNode("{ /*TODO*/ }"), true)
            }
            else {
              addVariable(EmptyExpression(), true)
            }
          }
          addTextSegment(")")
        }
        !insertLambda -> addTextSegment("()")
        requiredParameters.size < allParameters.size -> {
          addTextSegment("(")
          addVariable(EmptyExpression(), true)
          addTextSegment(")")
        }
      }

      if (insertLambda && !isNextElementOpenCurlyBrace()) {
        addTextSegment(" {\n")
        addEndVariable()
        addTextSegment("\n}")
      }
    }

    templateManager.startTemplate(editor, template, object : TemplateEditingAdapter() {
      override fun templateFinished(template: Template, brokenOff: Boolean) {
        if (!brokenOff) {
          val callExpression = file.findElementAt(editor.caretModel.offset)?.parentOfType<KtCallExpression>() ?: return
          val valueArgumentList = callExpression.valueArgumentList ?: return
          if (valueArgumentList.arguments.isEmpty() && callExpression.lambdaArguments.isNotEmpty()) {
            runWriteAction { valueArgumentList.delete() }
          }
        }
      }
    })
  }
}
