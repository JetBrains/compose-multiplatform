/*
 * Copyright (C) 2019 The Android Open Source Project
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

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jdom.Element

/**
 * [EntryPoint] implementation to mark `@Preview` functions as entry points and avoid them being flagged as unused.
 *
 * Based on
 * com.android.tools.idea.compose.preview.PreviewEntryPoint from AOSP
 * with modifications
 */
class PreviewEntryPoint : EntryPoint() {
    private var ADD_PREVIEW_TO_ENTRIES: Boolean = true

    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement): Boolean = isEntryPoint(psiElement)

    override fun isEntryPoint(psiElement: PsiElement): Boolean =
        psiElement is PsiMethod && psiElement.hasAnnotation(DESKTOP_PREVIEW_ANNOTATION_FQN)

    override fun readExternal(element: Element) = element.deserializeInto(this)

    override fun writeExternal(element: Element) {
        serializeObjectInto(this, element)
    }

    override fun getDisplayName(): String = "Compose Preview"

    override fun isSelected(): Boolean = ADD_PREVIEW_TO_ENTRIES

    override fun setSelected(selected: Boolean) {
        this.ADD_PREVIEW_TO_ENTRIES = selected
    }
}