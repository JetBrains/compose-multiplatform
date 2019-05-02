/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.editor

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