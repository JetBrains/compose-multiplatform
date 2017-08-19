package org.jetbrains.kotlin.r4a.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.r4a.R4aSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.r4a.GeneratedViewClassDescriptor
import org.jetbrains.kotlin.r4a.PackageAnalysisHandlerExtension
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import java.util.ArrayList
import java.util.HashSet

class R4aIdeSyntheticPackageFragmentProvider(private val project: Project, private val bindingTrace: BindingTrace, private val module: ModuleDescriptor) : PackageFragmentProvider {

    fun getClasses(): HashSet<KtFile> {
        val files = HashSet<KtFile>()

        ProjectFileIndex.SERVICE.getInstance(project).iterateContent(object : ContentIterator {
            override fun processFile(fileOrDir: VirtualFile): Boolean {
                if(!fileOrDir.name.endsWith(".kt")) return true;
                val file = KtFile(SingleRootFileViewProvider(PsiManager.getInstance(project), fileOrDir, false), false)
                files.add(file)
                return true
            }
        })

        return files
    }


    override fun getPackageFragments(packageName: FqName): List<PackageFragmentDescriptor>
    {
        PackageAnalysisHandlerExtension.doAnalysis(module, bindingTrace, getClasses());
        var descriptors = ArrayList<PackageFragmentDescriptor>()
        for(cls in bindingTrace.bindingContext.get(R4AWritableSlices.COMPONENT_CLASSES, module) ?: emptySet<KtClass>()) {
            if(cls.fqName?.parent() != packageName) continue
            val wrapperView = bindingTrace.bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, cls) ?: continue
            descriptors.add(wrapperView.containingDeclaration as PackageFragmentDescriptor)
        }

        return descriptors
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
}
