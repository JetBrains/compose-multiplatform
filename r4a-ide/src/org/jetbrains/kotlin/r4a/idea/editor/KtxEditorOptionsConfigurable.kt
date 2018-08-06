/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.editor

import com.intellij.openapi.options.BeanConfigurable
import com.intellij.openapi.options.UnnamedConfigurable

class KtxEditorOptionsConfigurable : BeanConfigurable<KtxEditorOptions>(KtxEditorOptions.getInstance()), UnnamedConfigurable {
    init {
        val instance = KtxEditorOptions.getInstance()
        checkBox("Convert pasted XML code to Kotlin KTX",
                 { instance.enableXmlToKtxConversion },
                 { instance.enableXmlToKtxConversion = it }
        )
        checkBox("Don't show XML to Kotlin KTX conversion dialog on paste",
                 { instance.donTShowKtxConversionDialog },
                 { instance.donTShowKtxConversionDialog = it })
    }
}