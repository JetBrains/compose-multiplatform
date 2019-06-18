/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.plugins.idea.editor

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