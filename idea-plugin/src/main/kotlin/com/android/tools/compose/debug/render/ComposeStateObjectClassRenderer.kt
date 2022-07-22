/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.compose.debug.render

import com.android.tools.compose.debug.render.ComposeStateObjectClassRenderer.Companion.DEBUGGER_DISPLAY_VALUE_METHOD_NAME
import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsAsync
import com.intellij.debugger.impl.DebuggerUtilsImpl
import com.intellij.debugger.settings.NodeRendererSettings
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.DebuggerTreeNode
import com.intellij.debugger.ui.tree.NodeDescriptor
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.CachedEvaluator
import com.intellij.debugger.ui.tree.render.ChildrenBuilder
import com.intellij.debugger.ui.tree.render.ClassRenderer
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.debugger.ui.tree.render.NodeRenderer
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants
import com.sun.jdi.ClassType
import com.sun.jdi.Type
import com.sun.jdi.Value
import org.jetbrains.kotlin.idea.debugger.KotlinClassRenderer
import org.jetbrains.kotlin.idea.debugger.core.isInKotlinSources
import java.util.concurrent.CompletableFuture

/**
 * Renderer for a given compose `StateObject` type object.
 *
 * Basically, for a given compose state object, its underlying value (by invoking [DEBUGGER_DISPLAY_VALUE_METHOD_NAME])
 * determines how it's rendered in the "Variables" pane. This is to provide an auto-unboxing experience while debugging,
 * that users can identify the data by a glance at this more readable data view.
 *
 * E.g.
 * 1) if the underlying value is an integer `1`, the label is rendered `1`.
 * 2) if the underlying value is a list, then the given object is rendered by a `List` renderer instead of the
 * original `Kotlin class` renderer. That is, "size = xx" is the label, and the `ArrayRenderer` is the children renderer
 * in this case.
 * 3) if the underlying value is a map, then the given object is rendered by a `Map` renderer instead of the original
 * `Kotlin class` renderer. That is, "size = xx" is the label, and the `ArrayRenderer` is the children renderer in
 * this case. When expanding, each of the entry is rendered by the `Map.Entry` renderer.
 *
 * @param fqcn the fully qualified class name of the Compose State Object to apply this custom renderer to.
 */
class ComposeStateObjectClassRenderer(private val fqcn: String) : ClassRenderer() {
  // We fallback to [KotlinClassRenderer] when the following exception is thrown:
  // Unable to evaluate the expression No such instance method: 'getDebuggerDisplayValue',
  private val fallbackRenderer by lazy {
    KotlinClassRenderer()
  }

  private val prioritizedCollectionRenderers by lazy {
    NodeRendererSettings.getInstance()
      .alternateCollectionRenderers
      .filter { it.name == "Map" || it.name == "List" }
      .filter { it.isEnabled }
      .toList()
  }

  private val debuggerDisplayValueEvaluator = DebuggerDisplayValueEvaluator(fqcn)

  init {
    setIsApplicableChecker { type: Type? ->
      if (type !is ClassType || !type.isInKotlinSources()) return@setIsApplicableChecker CompletableFuture.completedFuture(false)

      DebuggerUtilsAsync.instanceOf(type, fqcn)
    }
  }

  companion object {
    private val NODE_RENDERER_KEY = Key.create<NodeRenderer>(this::class.java.simpleName)

    // The name of the method we expect the Compose State Object to implement. We invoke it to retrieve the underlying
    // Compose State Object value.
    private const val DEBUGGER_DISPLAY_VALUE_METHOD_NAME = "getDebuggerDisplayValue"
  }

  override fun buildChildren(value: Value, builder: ChildrenBuilder, evaluationContext: EvaluationContext) {
    val debuggerDisplayValueDescriptor = try {
      getDebuggerDisplayValueDescriptor(value, evaluationContext, null)
    }
    catch (evaluateException: EvaluateException) {
      if (evaluateException.localizedMessage.startsWith("No such instance method:")) {
        return fallbackRenderer.buildChildren(value, builder, evaluationContext)
      }

      throw evaluateException
    }

    getDelegatedRendererAsync(evaluationContext.debugProcess, debuggerDisplayValueDescriptor)
      .thenAccept { renderer ->
        builder.parentDescriptor.putUserData(NODE_RENDERER_KEY, renderer)
        renderer.buildChildren(debuggerDisplayValueDescriptor.value, builder, evaluationContext)
      }
  }

  override fun getChildValueExpression(node: DebuggerTreeNode, context: DebuggerContext): PsiElement? {
    return node.parent.descriptor.getUserData(NODE_RENDERER_KEY)?.getChildValueExpression(node, context)
  }

  override fun isExpandableAsync(
    value: Value,
    evaluationContext: EvaluationContext,
    parentDescriptor: NodeDescriptor
  ): CompletableFuture<Boolean> {
    val debuggerDisplayValueDescriptor = try {
      getDebuggerDisplayValueDescriptor(value, evaluationContext, null)
    }
    catch (evaluateException: EvaluateException) {
      if (evaluateException.localizedMessage.startsWith("No such instance method:")) {
        return fallbackRenderer.isExpandableAsync(value, evaluationContext, parentDescriptor)
      }

      return CompletableFuture.failedFuture(evaluateException)
    }

    return getDelegatedRendererAsync(evaluationContext.debugProcess, debuggerDisplayValueDescriptor)
      .thenCompose { renderer ->
        renderer.isExpandableAsync(debuggerDisplayValueDescriptor.value, evaluationContext, debuggerDisplayValueDescriptor)
      }
  }

  /**
   * Returns a [ValueDescriptor] for the underlying "debugger display value", which is evaluated by invoking the
   * [DEBUGGER_DISPLAY_VALUE_METHOD_NAME] method of the Compose `StateObject` type object: [value].
   */
  private fun getDebuggerDisplayValueDescriptor(
    value: Value,
    evaluationContext: EvaluationContext,
    originalDescriptor: ValueDescriptor?
  ): ValueDescriptor {
    val debugProcess = evaluationContext.debugProcess

    if (!debugProcess.isAttached) throw EvaluateExceptionUtil.PROCESS_EXITED

    val thisEvaluationContext = evaluationContext.createEvaluationContext(value)
    val debuggerDisplayValue = debuggerDisplayValueEvaluator.evaluate(debugProcess.project, thisEvaluationContext)

    return object : ValueDescriptorImpl(evaluationContext.project, debuggerDisplayValue) {
      override fun getDescriptorEvaluation(context: DebuggerContext): PsiExpression? = null
      override fun calcValue(evaluationContext: EvaluationContextImpl?): Value = debuggerDisplayValue
      override fun calcValueName(): String = "value"

      override fun setValueLabel(label: String) {
        originalDescriptor?.setValueLabel(label)
      }
    }
  }

  /**
   * Return an ID of this renderer class, used by the IntelliJ platform to identify our renderer among all active
   * renderers in the system.
   */
  override fun getUniqueId(): String {
    return fqcn
  }

  override fun calcLabel(descriptor: ValueDescriptor, evaluationContext: EvaluationContext, listener: DescriptorLabelListener): String {
    val debuggerDisplayValueDescriptor: ValueDescriptor = try {
      getDebuggerDisplayValueDescriptor(descriptor.value, evaluationContext, descriptor)
    }
    catch (evaluateException: EvaluateException) {
      if (evaluateException.localizedMessage.startsWith("No such instance method:")) {
        return fallbackRenderer.calcLabel(descriptor, evaluationContext, listener)
      }

      throw evaluateException
    }

    val renderer = getDelegatedRendererAsync(evaluationContext.debugProcess, debuggerDisplayValueDescriptor)
    return calcLabelAsync(renderer, debuggerDisplayValueDescriptor, evaluationContext, listener)
      .getNow(XDebuggerUIConstants.getCollectingDataMessage())
  }

  private fun calcLabelAsync(
    renderer: CompletableFuture<NodeRenderer>,
    descriptor: ValueDescriptor,
    evaluationContext: EvaluationContext?,
    listener: DescriptorLabelListener
  ): CompletableFuture<String> {
    return renderer.thenApply { r: NodeRenderer ->
      try {
        val label = r.calcLabel(descriptor, evaluationContext, listener)
        descriptor.setValueLabel(label)
        listener.labelChanged()
        return@thenApply label
      }
      catch (evaluateException: EvaluateException) {
        descriptor.setValueLabelFailed(evaluateException)
        listener.labelChanged()
        return@thenApply ""
      }
    }
  }

  /**
   * Returns a [CompletableFuture] of the first applicable renderer for the given [valueDescriptor].
   */
  private fun getDelegatedRendererAsync(debugProcess: DebugProcess, valueDescriptor: ValueDescriptor): CompletableFuture<NodeRenderer> {
    val type = valueDescriptor.type
    return DebuggerUtilsImpl.getApplicableRenderers(prioritizedCollectionRenderers, type)
      .thenCompose { renderers ->
        // Return any applicable renderer of [prioritizedCollectionRenderers]. This is to de-prioritize `Kotlin class` renderer.
        // Or fallback to the default renderer.
        val found = renderers.firstOrNull() ?: return@thenCompose (debugProcess as DebugProcessImpl).getAutoRendererAsync(type)

        CompletableFuture.completedFuture(found)
      }
  }

  /**
   * [CachedEvaluator] used to invoke the [DEBUGGER_DISPLAY_VALUE_METHOD_NAME] method.
   */
  private class DebuggerDisplayValueEvaluator(private val fqcn: String) : CachedEvaluator() {
    init {
      referenceExpression = TextWithImportsImpl(
        CodeFragmentKind.EXPRESSION,
        "this.$DEBUGGER_DISPLAY_VALUE_METHOD_NAME()",
        "",
        JavaFileType.INSTANCE
      )
    }

    override fun getClassName(): String {
      return fqcn
    }

    fun evaluate(project: Project, context: EvaluationContext): Value {
      return getEvaluator(project).evaluate(context)
    }
  }
}