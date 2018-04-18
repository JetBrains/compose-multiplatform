package org.jetbrains.kotlin.r4a

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension


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
        AnalysisHandlerExtension.registerExtension(project, PackageAnalysisHandlerExtension())
        AnalysisHandlerExtension.registerExtension(project, R4aClassAnalisysHandlerExtension())
        AnalysisHandlerExtension.registerExtension(project, TrueFalseAnalysisHandlerExtension())
        PackageFragmentProviderExtension.registerExtension(project, R4aGradlePackageFragmentProviderExtension());
        KtxTypeResolutionExtension.registerExtension(project, R4aKtxTypeResolutionExtension())
        SyntheticResolveExtension.registerExtension(project, StaticWrapperCreatorFunctionResolveExtension())
        SyntheticResolveExtension.registerExtension(project, WrapperViewSettersGettersResolveExtension())
        ClassBuilderInterceptorExtension.registerExtension(project, ReflectiveFragmentInjectorInterceptorExtension())
     //   ClassBuilderInterceptorExtension.registerExtension(project, ComponentMutationRerenderInjectorInterceptorExtension())
        SyntheticIrExtension.registerExtension(project, R4ASyntheticIrExtension())
    }
}
