package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import java.util.HashMap

public class R4aGradleSyntheticPackageFragmentProvider(private val module: ModuleDescriptor, private val bindingContext: BindingContext) : PackageFragmentProvider {

    override fun getPackageFragments(packageName: FqName): List<PackageFragmentDescriptor>
    {
        var descriptors = ArrayList<PackageFragmentDescriptor>()
        for(cls in bindingContext.get(R4AWritableSlices.COMPONENT_CLASSES, module) ?: emptySet<KtClass>()) {
            if(cls.fqName?.parent() != packageName) continue
            val wrapperView = bindingContext.get(R4AWritableSlices.WRAPPER_VIEW, cls) ?: continue
            descriptors.add(wrapperView.containingDeclaration as PackageFragmentDescriptor)
        }

        return descriptors
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
}