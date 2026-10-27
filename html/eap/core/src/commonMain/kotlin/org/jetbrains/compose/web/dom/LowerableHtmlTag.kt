/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Access shared serialization helpers without exposing them as public API.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.attributes.InputType

/**
 * Internal compiler contract. Attribute helpers follow the attrs receiver and parameter type.
 * Only defaults and behavior not expressed by the signature need metadata.
 */
@ComposeWebInternalApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class LowerableHtmlTag(
    val tagName: String = "",
    val namespace: String = HtmlNamespace,
    // Non-null library defaults, encoded as "parameter=literal".
    val defaults: Array<String> = [],
    val afterAttrs: Array<String> = [],
    val text: String = "",
    val omitEmpty: Array<String> = [],
    val inputType: String = "",
)

/** A direct attribute helper. Names and toString conversions follow the signature by default. */
@ComposeWebInternalApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class LowerableAttribute(
    val name: String = "",
    val conversion: String = "",
    val value: String = "",
    // Serialized argument default; an empty array requires an explicit argument.
    val defaultValue: Array<String> = [],
)

@PublishedApi
internal fun htmlAutoCompleteValue(value: Boolean): String = if (value) "on" else "off"

@PublishedApi
internal fun inputValueIsAttribute(type: InputType<*>): Boolean =
    type == InputType.Checkbox ||
            type == InputType.Radio ||
            type == InputType.Hidden ||
            type == InputType.Submit
