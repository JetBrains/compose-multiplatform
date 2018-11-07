package org.jetbrains.kotlin.r4a

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension

class R4ACommandLineProcessor : CommandLineProcessor {

    companion object {
        val PLUGIN_ID = "org.jetbrains.kotlin.r4a"
    }

    override val pluginId = PLUGIN_ID
    override val pluginOptions = emptyList<CliOption>()

    override fun processOption(option: CliOption, value: String, configuration: CompilerConfiguration) = when (option) {
        else -> throw CliOptionProcessingException("Unknown option: ${option.name}")
    }
}

class R4AComponentRegistrar : ComponentRegistrar {

    companion object {
        val COMPOSABLE_CHECKER_MODE_KEY = CompilerConfigurationKey<ComposableAnnotationChecker.Mode>("@composable checker mode")
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerProjectComponents(project as Project, configuration)
    }

    fun registerProjectComponents(project: Project, configuration: CompilerConfiguration) {
        StorageComponentContainerContributor.registerExtension(project, ComponentsClosedDeclarationChecker())
        StorageComponentContainerContributor.registerExtension(project, ComposableAnnotationChecker(configuration.get(COMPOSABLE_CHECKER_MODE_KEY, ComposableAnnotationChecker.DEFAULT_MODE)))
        StorageComponentContainerContributor.registerExtension(project, UnionAnnotationCheckerProvider())
        KtxParsingExtension.registerExtension(project, R4aKtxParsingExtension())
        KtxTypeResolutionExtension.registerExtension(project, R4aKtxTypeResolutionExtension())
        KtxControlFlowExtension.registerExtension(project, R4aKtxControlFlowExtension())
        R4aDiagnosticSuppressor.registerExtension(project, R4aDiagnosticSuppressor())
        TypeResolutionInterceptorExtension.registerExtension(project, R4aTypeResolutionInterceptorExtension())
        SyntheticIrExtension.registerExtension(project, R4ASyntheticIrExtension())
    }
}

