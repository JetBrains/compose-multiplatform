/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.web.ide.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.compose.common.modulePath
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants

class WebRunConfigurationProducer : LazyRunConfigurationProducer<GradleRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        GradleExternalTaskConfigurationType.getInstance().factory

    override fun isConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val mainFun = context.jsMainOrNull ?: return false
        return configuration.run {
                name == mainFun.name!!
                    && settings.externalProjectPath == context.modulePath()
                    && settings.taskNames.contains(jsRunTaskName)
        }
    }

    override fun setupConfigurationFromContext(
        configuration: GradleRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val mainFun = context.jsMainOrNull ?: return false
        configuration.apply {
            name = mainFun.name!!
            settings.taskNames.add(jsRunTaskName)
            settings.externalProjectPath =
                ExternalSystemApiUtil.getExternalProjectPath(context.location?.module)
        }
        return true
    }

    companion object {
        private const val jsRunTaskName = "jsBrowserDevelopmentRun"
    }
}

private val ConfigurationContext.jsMainOrNull: KtNamedFunction?
    get() = psiLocation?.parent?.getAsJsMainFunctionOrNull()