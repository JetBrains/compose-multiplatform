/*
 * Copyright (C) 2019 The Android Open Source Project
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

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.compose.common.modulePath
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

/**
 * Producer of [ComposePreviewRunConfiguration] for `@Composable` functions annotated with [PREVIEW_ANNOTATION_FQN]. The configuration
 * created is initially named after the `@Composable` function, and its fully qualified name is properly set in the configuration.
 *
 * The [ConfigurationContext] where the [ComposePreviewRunConfiguration] is created from can be any descendant of the `@Composable` function
 * in the PSI tree, such as its annotations, function name or even the keyword "fun".
 *
 * Based on com.android.tools.idea.compose.preview.runconfiguration.ComposePreviewRunConfigurationProducer from AOSP
 * with modifications
 */
class PreviewRunConfigurationProducer : LazyRunConfigurationProducer<GradleRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        GradleExternalTaskConfigurationType.getInstance().factory

    override fun isConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val composeFunction = context.containingComposePreviewFunction() ?: return false

        return configuration.run {
            name == composeFunction.name!!
                && settings.externalProjectPath == context.modulePath()
                && settings.scriptParameters.contains(
                    previewTargetGradleArg(composeFunction.composePreviewFunctionFqn())
                )
        }
    }

    override fun setupConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val composeFunction = context.containingComposePreviewFunction() ?: return false

        configuration.apply {
            name = composeFunction.name!!
            settings.taskNames.add("runComposeDesktopPreview")
            settings.externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(context.location?.module)
            settings.scriptParameters = listOf(
                previewTargetGradleArg(composeFunction.composePreviewFunctionFqn())
            ).joinToString(" ")
        }
        return true
    }
}

private fun previewTargetGradleArg(target: String): String =
    "-Pcompose.desktop.preview.target=$target"

private fun KtNamedFunction.composePreviewFunctionFqn() = "${getClassName()}.${name}"

private fun ConfigurationContext.containingComposePreviewFunction() =
    psiLocation?.let { location -> location.getNonStrictParentOfType<KtNamedFunction>()?.takeIf { it.isValidComposePreview() } }