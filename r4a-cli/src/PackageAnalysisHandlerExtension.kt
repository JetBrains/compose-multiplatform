package org.jetbrains.kotlin.r4a

import com.intellij.openapi.project.Project
import com.sun.jndi.ldap.LdapPoolManager.trace
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.util.*

open class PackageAnalysisHandlerExtension : AnalysisHandlerExtension {

    companion object {
        fun doAnalysis(module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>) {
            val classes = HashSet<KtClass>();
            for (file in files) {
                for(declaration in file.declarations) {
                    declaration as? KtClass ?: continue
                    classes.add(declaration)
                    if(!R4aSyntheticPackageFragmentDescriptor.isR4aComponent(declaration)) continue
                    val wrapperView = GeneratedViewClassDescriptor(module, declaration, bindingTrace.bindingContext)
                    bindingTrace.record(R4AWritableSlices.WRAPPER_VIEW, declaration, wrapperView)
                }
            }

            bindingTrace.record(R4AWritableSlices.COMPONENT_CLASSES, module, classes)
        }
    }

    override fun doAnalysis(
            project: Project,
            module: ModuleDescriptor,
            projectContext: ProjectContext,
            files: Collection<KtFile>,
            bindingTrace: BindingTrace,
            componentProvider: ComponentProvider
    ): AnalysisResult? {
        doAnalysis(module, bindingTrace, files);
        return null
    }
}
