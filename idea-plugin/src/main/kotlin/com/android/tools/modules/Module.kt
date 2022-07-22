/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.android.tools.modules

import com.intellij.openapi.module.Module
import com.intellij.psi.*

fun PsiElement.inComposeModule() = true
fun Module.isComposeModule() = true