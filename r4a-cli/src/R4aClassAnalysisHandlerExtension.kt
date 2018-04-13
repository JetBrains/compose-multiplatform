package org.jetbrains.kotlin.r4a

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class R4aClassAnalisysHandlerExtension : AnalysisHandlerExtension {
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        for (file in files) {
            for (declaration in file.declarations) {
                val classDeclaration = declaration as? KtClass ?: continue
                if (!R4aSyntheticPackageFragmentDescriptor.isR4aComponent(classDeclaration)) continue
                if (classDeclaration.hasModifier(KtTokens.OPEN_KEYWORD) || classDeclaration.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                    val element = classDeclaration.nameIdentifier ?: classDeclaration
                    bindingTrace.reportFromPlugin(R4AErrors.OPEN_COMPONENT.on(element), R4ADefaultErrorMessages)
                }
            }
        }
        return null
    }
}