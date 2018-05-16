package org.jetbrains.kotlin.r4a

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.r4a.frames.FrameTransformExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension


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
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        StorageComponentContainerContributor.registerExtension(project, ComponentsClosedDeclarationChecker())
        StorageComponentContainerContributor.registerExtension(project, ComposableAnnotationChecker())
        KtxTypeResolutionExtension.registerExtension(project, R4aKtxTypeResolutionExtension())
        SyntheticResolveExtension.registerExtension(project, StaticWrapperCreatorFunctionResolveExtension())
        SyntheticResolveExtension.registerExtension(project, WrapperViewSettersGettersResolveExtension())
        SyntheticIrExtension.registerExtension(project, R4ASyntheticIrExtension())
        SyntheticIrExtension.registerExtension(project, FrameTransformExtension())
    }
}

