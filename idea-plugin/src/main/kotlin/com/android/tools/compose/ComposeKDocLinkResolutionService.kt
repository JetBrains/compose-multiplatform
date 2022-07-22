/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.compose

import com.android.tools.idea.flags.StudioFlags
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.kdoc.IdeKDocLinkResolutionService
import org.jetbrains.kotlin.idea.kdoc.KDocLinkResolutionService
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.base.projectStructure.scope.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

/**
 * Resolves links to functions and classes inside KDoc that are not included to the project (as byte code).
 *
 * It's a copy of [org.jetbrains.kotlin.idea.kdoc.IdeKDocLinkResolutionService], but with a larger search scope:
 * GlobalSearchScope.everythingScope(project) instead of GlobalSearchScope.projectScope(project).
 * Source code is already in the index, it attached in [AndroidModuleDependenciesSetup#setUpLibraryDependency]
 */
class ComposeKDocLinkResolutionService : KDocLinkResolutionService {
  override fun resolveKDocLink(
    context: BindingContext,
    fromDescriptor: DeclarationDescriptor,
    resolutionFacade: ResolutionFacade,
    qualifiedName: List<String>
  ): Collection<DeclarationDescriptor> {
    val project = resolutionFacade.project
    val descriptors = IdeKDocLinkResolutionService(project).resolveKDocLink(context, fromDescriptor, resolutionFacade, qualifiedName)

    if (!StudioFlags.SAMPLES_SUPPORT_ENABLED.get()) return descriptors

    val scope = KotlinSourceFilterScope.librarySources(GlobalSearchScope.everythingScope(project), project)

    val shortName = qualifiedName.lastOrNull() ?: return emptyList()
    val targetFqName = FqName.fromSegments(qualifiedName)

    val functions = KotlinFunctionShortNameIndex.get(shortName, project, scope).asSequence()
    val classes = KotlinClassShortNameIndex.get(shortName, project, scope).asSequence()

    val additionalDescriptors = (functions + classes)
      .filter { it.fqName == targetFqName }
      .map { it.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) } // TODO Filter out not visible due dependencies config descriptors
      .toList()
    if (additionalDescriptors.isNotEmpty())
      return additionalDescriptors + descriptors

    return descriptors
  }
}
