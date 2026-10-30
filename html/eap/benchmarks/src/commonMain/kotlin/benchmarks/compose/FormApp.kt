/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package benchmarks.compose

import benchmarks.model.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import androidx.compose.runtime.Composable

@Composable
fun FormAppDocument(data: FormData) {
    Html(attrs = { lang("en") }) {
        Head {
            Meta(attrs = { attr("charset", "UTF-8") })
            Title { Text(data.title) }
        }
        Body {
            FormApp(data)
        }
    }
}

@Composable
fun FormApp(data: FormData) {
    Div(attrs = {
        id("app-container")
        attr("role", "main")
    }) {
        H1 { Text(data.title) }
        Form(action = data.actionUrl, attrs = {
            method(FormMethod.Post)
            attr("aria-label", data.formLabel)
        }) {
            data.sections.forEach { section ->
                Fieldset(attrs = {
                    attr("aria-labelledby", "legend-${section.id}")
                }) {
                    Legend(attrs = { id("legend-${section.id}") }) { Text(section.title) }

                    section.fields.forEach { field ->
                        Div(attrs = {
                            classes("form-group")
                            id("group-${field.id}")
                        }) {
                            Label(forId = field.id) { Text(field.label) }

                            val hasError = field.errorMessage != null
                            val ariaDescribedBy = buildString {
                                if (field.helpText != null) append("help-${field.id} ")
                                if (hasError) append("error-${field.id}")
                            }.trim()

                            when (field.type) {
                                FormFieldType.TEXT, FormFieldType.EMAIL, FormFieldType.PASSWORD, FormFieldType.NUMBER, FormFieldType.TEL -> {
                                    Input(type = when(field.type) {
                                        FormFieldType.EMAIL -> InputType.Email
                                        FormFieldType.PASSWORD -> InputType.Password
                                        FormFieldType.NUMBER -> InputType.Number
                                        FormFieldType.TEL -> InputType.Tel
                                        else -> InputType.Text
                                    }) {
                                        id(field.id)
                                        name(field.name)
                                        field.value?.let { attr("value", it) }
                                        field.placeholder?.let { placeholder(it) }
                                        if (field.required) required()
                                        if (field.disabled) disabled()
                                        if (field.readOnly) readOnly()
                                        field.autoComplete?.let { attr("autocomplete", it) }

                                        field.min?.let { min(it) }
                                        field.max?.let { max(it) }
                                        field.minLength?.let { attr("minlength", it.toString()) }
                                        field.maxLength?.let { attr("maxlength", it.toString()) }
                                        field.pattern?.let { attr("pattern", it) }
                                        field.step?.let { attr("step", it.toString().removeSuffix(".0")) }

                                        if (field.required) attr("aria-required", "true")
                                        if (hasError) attr("aria-invalid", "true")
                                        if (ariaDescribedBy.isNotEmpty()) attr("aria-describedby", ariaDescribedBy)
                                    }
                                }
                                FormFieldType.SELECT -> {
                                    Select(attrs = {
                                        id(field.id)
                                        name(field.name)
                                        if (field.required) required()
                                        if (field.disabled) disabled()
                                        if (field.multiple) multiple()

                                        if (field.required) attr("aria-required", "true")
                                        if (hasError) attr("aria-invalid", "true")
                                        if (ariaDescribedBy.isNotEmpty()) attr("aria-describedby", ariaDescribedBy)
                                    }) {
                                        field.options?.forEach { option ->
                                            Option(value = option.value, attrs = {
                                                if (option.selected) selected()
                                                if (option.disabled) disabled()
                                            }) { Text(option.label) }
                                        }
                                    }
                                }
                                FormFieldType.TEXTAREA -> {
                                    TagElement<kotlinx.browser.dom.HTMLTextAreaElement>(
                                        elementBuilder = ElementBuilder.createBuilder("textarea"),
                                        applyAttrs = {
                                            id(field.id)
                                            attr("name", field.name)
                                            field.placeholder?.let { attr("placeholder", it) }
                                            if (field.required) attr("required", "")
                                            if (field.disabled) attr("disabled", "")
                                            if (field.readOnly) attr("readonly", "")
                                            field.minLength?.let { attr("minlength", it.toString()) }
                                            field.maxLength?.let { attr("maxlength", it.toString()) }
                                            if (field.required) attr("aria-required", "true")
                                            if (hasError) attr("aria-invalid", "true")
                                            if (ariaDescribedBy.isNotEmpty()) attr("aria-describedby", ariaDescribedBy)
                                        },
                                        content = { Text(field.value ?: "") },
                                    )
                                }
                                FormFieldType.RADIO -> {
                                    Div(attrs = {
                                        attr("role", "radiogroup")
                                        if (field.required) attr("aria-required", "true")
                                        if (hasError) attr("aria-invalid", "true")
                                        if (ariaDescribedBy.isNotEmpty()) attr("aria-describedby", ariaDescribedBy)
                                    }) {
                                        field.options?.forEachIndexed { index, option ->
                                            val optId = "${field.id}-opt-$index"
                                            Div {
                                                Input(type = InputType.Radio) {
                                                    id(optId)
                                                    name(field.name)
                                                    attr("value", option.value)
                                                    if (option.selected) attr("checked", "")
                                                    if (option.disabled) disabled()
                                                }
                                                Label(forId = optId) { Text(option.label) }
                                            }
                                        }
                                    }
                                }
                                FormFieldType.CHECKBOX -> {
                                    Input(type = InputType.Checkbox) {
                                        id(field.id)
                                        name(field.name)
                                        field.value?.let { attr("value", it) }
                                        if (field.checked) attr("checked", "")
                                        if (field.required) required()
                                        if (field.disabled) disabled()

                                        if (field.required) attr("aria-required", "true")
                                        if (hasError) attr("aria-invalid", "true")
                                        if (ariaDescribedBy.isNotEmpty()) attr("aria-describedby", ariaDescribedBy)
                                    }
                                }
                            }

                            field.helpText?.let { help ->
                                Span(attrs = {
                                    id("help-${field.id}")
                                    classes("help-text")
                                }) { Text(help) }
                            }

                            if (hasError) {
                                Div(attrs = {
                                    id("error-${field.id}")
                                    classes("error-message")
                                    attr("aria-live", "polite")
                                }) { Text(field.errorMessage) }
                            }
                        }
                    }
                }
            }
            Button(attrs = {
                type(ButtonType.Submit)
                if (data.isSubmitting) disabled()
            }) { Text("Submit Registration") }
        }
    }
}
