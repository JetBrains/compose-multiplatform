package org.jetbrains.kotlin.r4a

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.storage.StorageManager
import java.util.HashMap


class R4aGradlePackageFragmentProviderExtension() : PackageFragmentProviderExtension {
    override fun getPackageFragmentProvider(project: Project, module: ModuleDescriptor, storageManager: StorageManager, trace: BindingTrace, moduleInfo: ModuleInfo?, lookupTracker: LookupTracker): PackageFragmentProvider? {
        return R4aGradleSyntheticPackageFragmentProvider(module, trace.bindingContext)
    }
}
