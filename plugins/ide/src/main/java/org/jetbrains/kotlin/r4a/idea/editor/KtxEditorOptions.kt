/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.editor

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "KtxEditorOptions", storages = [Storage(file = "\$APP_CONFIG$/ktx.xml")])
class KtxEditorOptions : PersistentStateComponent<KtxEditorOptions> {
    companion object {
        fun getInstance(): KtxEditorOptions {
            return ServiceManager.getService(KtxEditorOptions::class.java)
        }
    }

    var enableXmlToKtxConversion = true
    var enableAddComposableAnnotation = true
    var donTShowKtxConversionDialog = false
    var donTShowAddComposableAnnotationDialog = false

    override fun getState(): KtxEditorOptions? {
        return this
    }

    override fun loadState(state: KtxEditorOptions) {
        XmlSerializerUtil.copyBean(state, this)
    }
}