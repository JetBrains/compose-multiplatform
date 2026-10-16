/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

/**
 * Internal compiler contract for a plain HTML tag wrapper.
 *
 * The function forwards nullable attrs and optional content unchanged, in that order,
 * without additional behavior. The HTML name determines serialization rules.
 * [tagName] defaults to the lowercase function name.
 *
 * Applications should not apply this annotation. It is not a supported extension API.
 */
@ComposeWebInternalApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class LowerableHtmlTag(val tagName: String = "")
