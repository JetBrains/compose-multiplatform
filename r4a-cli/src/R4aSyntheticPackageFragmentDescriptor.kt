package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.utils.Printer

class R4aSyntheticPackageFragmentDescriptor: PackageFragmentDescriptorImpl {
    private val scope = R4aScope()
    private val generatedViewClassDescriptor : GeneratedViewClassDescriptor

    constructor(module: ModuleDescriptor, packageName: FqName, wrapperViewDescriptor: GeneratedViewClassDescriptor, bindingContext: BindingContext): super(module, packageName) {
        generatedViewClassDescriptor = wrapperViewDescriptor
    }

    override fun getMemberScope(): MemberScope {
        return scope
    }

    fun getSyntheticWrapperViewClassDescriptor() = generatedViewClassDescriptor

    companion object {
        fun isR4aComponent(declaration: KtClass) : Boolean {

            // TODO: Check that the resolved type is com.google.r4a.Component (we are currently only checking that it matches the short name "Component")
            if(!(declaration as KtClass).getSuperNames().contains("Component")) return false

            /*
            TODO: do something like this
            Logger.log("declaratoin fqname: "+(declaration as KtClass).fqName)
            Logger.log("declaratoin type: "+(declaration as KtClass).getType(bindingTrace.bindingContext))
            for(entry in (declaration as KtClass).superTypeListEntries)
            {
                Logger.log("  declaratoin supertype entries: "+entry.typeReference.toString())
                Logger.log("  declaratoin supertype entriec class: "+entry.javaClass)
                Logger.log("  declaratoin supertype entriec class: "+ resolveSession.resolveToDescriptor(entry as KtSuperTypeCallEntry))
                Logger.log("  declaratoin supertype entriec typrereference class: "+entry.typeReference?.typeElement)
            }
            Logger.log("declaratoin supernames: "+)
            Logger.log("declaration class is: "+declaration.javaClass)
            val descriptor = resolveSession.resolveToDescriptor(declaration) as? ClassDescriptor ?: continue;

     Logger.log("prealing with: "+" "+descriptor.fqNameSafe+" "+descriptor.getSuperClassNotAny()+" "+descriptor.getSuperClassNotAny()?.fqNameSafe)
      Logger.log(descriptor.getSuperClassNotAny()?.fqNameSafe != FqName("com.google.r4a.Component"))
     if(descriptor.getSuperClassNotAny()?.fqNameSafe != FqName("com.google.r4a.Component")) continue
 */
            val name = declaration.fqName ?: return false  // TODO: In what circumstances could a KtClass have a null fqName?  Add a comment explaining.

            return true

        }
    }

    private inner class R4aScope : MemberScope {

        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<GeneratedViewClassDescriptor> {
            if(nameFilter(generatedViewClassDescriptor.name)) {
                return listOf(generatedViewClassDescriptor)
            }
            else return emptyList();
        }

        override fun getClassifierNames(): Set<Name>? = null

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
        {
            // TODO: This feels hacky; can a PackageFragmentDescriptor really only return a single class descriptor?
            // TODO: Is this really the right place to be returning this, or is it mostly an accident that it works?
            // TODO: Are sure this fqName check is actually doing its intended job? (IIRC, it is not filtering the fqName properly, but I don't remember the exact bug)
            if (name.isSpecial) return null
            if(generatedViewClassDescriptor.fqNameSafe == FqName(fqName.toString()+"."+name.identifier))
                return generatedViewClassDescriptor;
            return null
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
        {
            return emptyList()
        }

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor>
        {
            return emptyList()
        }

        override fun getFunctionNames(): Set<Name> {
            return emptySet()
        }

        override fun getVariableNames(): Set<Name> {
            return emptySet()
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName)
        }
    }
}
