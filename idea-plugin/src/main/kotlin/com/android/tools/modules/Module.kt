/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.android.tools.modules

import com.android.tools.compose.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import com.intellij.psi.*
import com.intellij.psi.search.*
import com.intellij.psi.util.*
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex

fun PsiElement.inComposeModule() = module?.isComposeModule() ?: false
fun Module.isComposeModule(): Boolean {
    return CachedValuesManager.getManager(project).getCachedValue(this) {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(this)
        val hasComposable = COMPOSABLE_FQ_NAMES.any {
            KotlinFullClassNameIndex.get(it, project, scope).any() 
        }
        val rootModificationTracker = ProjectRootModificationTracker.getInstance(project)
        CachedValueProvider.Result.create(hasComposable, rootModificationTracker)
    }
} 
    
