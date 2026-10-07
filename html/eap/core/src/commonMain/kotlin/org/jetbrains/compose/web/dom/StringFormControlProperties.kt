/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.setCheckedValue
import org.jetbrains.compose.web.attributes.setInputValue
import org.jetbrains.compose.web.attributes.setTextAreaDefaultValue
import org.jetbrains.compose.web.attributes.setTextAreaValue

/** Null fields preserve attributes or children; empty text replaces children. */
internal class FormControlState(
    val value: String? = null,
    val checked: Boolean? = null,
    val textContent: String? = null,
)

internal fun Map<String, String>.withFormState(state: FormControlState?): Map<String, String> {
    if (state == null || (state.value == null && state.checked == null)) return this
    return LinkedHashMap(this).apply {
        state.value?.let { put("value", it) }
        // Controlled false removes even an explicitly supplied checked attribute.
        when (state.checked) {
            true -> put("checked", "")
            false -> remove("checked")
            null -> Unit
        }
    }
}

/** Reads known form setters without invoking DOM callbacks or changing the attribute builder. */
internal fun List<Pair<(Element, Any) -> Unit, Any>>.formControlState(
    tagName: String,
    namespace: String?,
): FormControlState? {
    if (isEmpty() || namespace != HtmlNamespace) return null

    return when (tagName) {
        "input" -> {
            var inputValue: String? = null
            var checked: Boolean? = null
            for ((update, value) in this) {
                when {
                    update === setInputValue -> inputValue = value as String
                    update === setCheckedValue -> checked = value as Boolean
                }
            }
            if (inputValue == null && checked == null) null else FormControlState(inputValue, checked)
        }
        "textarea" -> {
            var textAreaValue: String? = null
            var textAreaDefaultValue: String? = null
            for ((update, value) in this) {
                when {
                    update === setTextAreaValue -> textAreaValue = value as String
                    update === setTextAreaDefaultValue -> textAreaDefaultValue = value as String
                }
            }
            // A controlled value takes precedence over a default, including an explicitly empty value.
            (textAreaValue ?: textAreaDefaultValue)?.let { FormControlState(textContent = it) }
        }
        else -> null
    }
}
