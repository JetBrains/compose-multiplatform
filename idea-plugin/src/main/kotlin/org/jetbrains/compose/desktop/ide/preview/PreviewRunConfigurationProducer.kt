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
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.service
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
            name == runConfigurationNameFor(composeFunction)
                    && settings.externalProjectPath == context.modulePath()
                    && settings.taskNames.singleOrNull() == configureDesktopPreviewTaskName
                    && settings.scriptParameters.split(" ").containsAll(
                        runConfigurationScriptParameters(composeFunction.composePreviewFunctionFqn(), context.port)
                    )
        }
    }

    override fun setupConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val composeFunction = context.containingComposePreviewFunction() ?: return false
        // todo: temporary configuration?
        configuration.apply {
            name = runConfigurationNameFor(composeFunction)
            settings.taskNames.add(configureDesktopPreviewTaskName)
            settings.externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(context.location?.module)
            settings.scriptParameters =
                runConfigurationScriptParameters(composeFunction.composePreviewFunctionFqn(), context.port)
                    .joinToString(" ")
        }

        return true
    }
}

private val configureDesktopPreviewTaskName = "configureDesktopPreview"

private fun runConfigurationNameFor(function: KtNamedFunction): String =
    "Configure Desktop Preview: ${function.name!!}"

private fun runConfigurationScriptParameters(target: String, idePort: Int): List<String> =
    listOf(
        "-Pcompose.desktop.preview.target=$target",
        "-Pcompose.desktop.preview.ide.port=${idePort}"
    )

private val ConfigurationContext.port: Int
    get() = project.service<PreviewStateService>().gradleCallbackPort

private fun KtNamedFunction.composePreviewFunctionFqn() = "${getClassName()}.${name}"

private fun ConfigurationContext.containingComposePreviewFunction() =
    psiLocation?.let { location -> location.getNonStrictParentOfType<KtNamedFunction>()?.takeIf { it.isValidComposePreview() } }