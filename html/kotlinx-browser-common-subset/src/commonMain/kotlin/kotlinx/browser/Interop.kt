/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.browser

import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List

/**
 * Common marker actualized to each target's JavaScript root type.
 *
 * ```kotlin
 * val raw: JsAny = media.getStartDate()
 * ```
 */
public expect interface JsAny

/**
 * A JavaScript string, which stays distinct from [String] on Wasm/JS.
 *
 * ```kotlin
 * val token: JsString? = tokens.item(0)
 * ```
 */
public expect class JsString : JsAny

public expect fun String.toJsString(): JsString

public expect fun JsString.toKotlinString(): String

/**
 * A JavaScript number, which stays distinct from the Kotlin numeric types on Wasm/JS.
 *
 * ```kotlin
 * val timestamp: JsNumber = event.timeStamp
 * ```
 */
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY")
public expect abstract class JsNumber : JsAny

public expect fun Double.toJsNumber(): JsNumber

public expect fun JsNumber.toDouble(): Double

/**
 * A numeric Web IDL sequence element: [Double] on JS and [JsNumber] on Wasm/JS.
 *
 * ```kotlin
 * val dash = listOf(1.25.toJsDouble(), 2.5.toJsDouble()).toJsArray()
 * context.setLineDash(dash)
 * ```
 */
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY")
public expect class JsDouble : JsAny

public expect fun Double.toJsDouble(): JsDouble

public expect fun JsDouble.toKotlinDouble(): Double

/**
 * Common JavaScript array identity. Use [toJsArray] to create one.
 *
 * ```kotlin
 * val filter: JsArray<JsString> = listOf("class".toJsString()).toJsArray()
 * val options = MutationObserverInit(attributeFilter = filter)
 * ```
 */
public expect class JsArray<T : JsAny?> : JsAny

public expect val JsArray<*>.length: Int

public expect operator fun <T : JsAny?> JsArray<T>.`get`(index: Int): T?

public expect operator fun <T : JsAny?> JsArray<T>.`set`(index: Int, `value`: T)

public expect fun <T : JsAny?> JsArray<T>.toList(): List<T>

public expect fun <T : JsAny?> List<T>.toJsArray(): JsArray<T>

/**
 * Opaque promise identity with star-projected results across web targets.
 *
 * ```kotlin
 * val pending: Promise<*> = element.requestFullscreen()
 * ```
 */
public expect class Promise<out T : JsAny?> : JsAny
