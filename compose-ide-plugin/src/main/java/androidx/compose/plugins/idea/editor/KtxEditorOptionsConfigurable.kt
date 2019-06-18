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

import com.intellij.openapi.options.BeanConfigurable
import com.intellij.openapi.options.UnnamedConfigurable

class KtxEditorOptionsConfigurable : BeanConfigurable<KtxEditorOptions>(
    KtxEditorOptions.getInstance()
), UnnamedConfigurable {
    init {
        val instance = KtxEditorOptions.getInstance()
        checkBox("Convert pasted XML code to Kotlin KTX",
            { instance.enableXmlToKtxConversion }, { instance.enableXmlToKtxConversion = it }
        )
        checkBox("Don't show XML to Kotlin KTX conversion dialog on paste",
            { instance.donTShowKtxConversionDialog },
            { instance.donTShowKtxConversionDialog = it })
        checkBox("Automatically add @Composable annotation and import androidx.compose.* when " +
            "converting XML to KTX code",
            {
                instance.enableAddComposableAnnotation &&
                        instance.donTShowAddComposableAnnotationDialog
            },
            { shouldAutomaticallyAddAnnotation ->
                if (shouldAutomaticallyAddAnnotation) {
                    instance.enableAddComposableAnnotation = true
                    instance.donTShowAddComposableAnnotationDialog = true
                } else {
                    // Unchecking the box means that on next paste, the dialog will be shown and the user will be able to cancel it and
                    // check "Don't show this dialog next time".
                    instance.enableAddComposableAnnotation = true
                    instance.donTShowAddComposableAnnotationDialog = false
                }
            }
        )
    }
}