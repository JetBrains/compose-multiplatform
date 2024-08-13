/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi

@ExperimentalComposeWebApi
data class Transition(
	var property: String? = null,
	var duration: CSSSizeValue<out CSSUnitTime>? = null,
	var timingFunction: AnimationTimingFunction? = null,
	var delay: CSSSizeValue<out CSSUnitTime>? = null,
) {
	override fun toString(): String {
		if (property == null) return ""
		var result = property!!
		
		duration?.let { result += " $it" }
		timingFunction?.let { result += " $it" }
		delay?.let { result += " $it" }
		
		return result
	}
}

@ExperimentalComposeWebApi
fun Transition.duration(value: CSSSizeValue<out CSSUnitTime>): Unit = run { duration = value }

@ExperimentalComposeWebApi
fun Transition.timingFunction(value: AnimationTimingFunction): Unit = run { timingFunction = value }

@ExperimentalComposeWebApi
fun Transition.delay(value: CSSSizeValue<out CSSUnitTime>): Unit = run { delay = value }

@ExperimentalComposeWebApi
data class Transitions(
	var transitions: List<Transition> = emptyList(),
	private var defaultDuration: CSSSizeValue<out CSSUnitTime>? = null,
	private var defaultTimingFunction: AnimationTimingFunction? = null,
	private var defaultDelay: CSSSizeValue<out CSSUnitTime>? = null,
) {
	override fun toString() =
		transitions.distinctBy {
			it.property
		}.joinToString(", ") {
			it.apply {
				if (defaultDelay != null && delay == null) delay = defaultDelay!!
				if (defaultDuration != null && duration == null) duration = defaultDuration!!
				if (defaultTimingFunction != null && timingFunction == null) timingFunction = defaultTimingFunction!!
			}.toString()
		}
	
	inline operator fun String.invoke(block: Transition.() -> Unit): Unit = Transition().apply(block).let {
		it.property = this
		transitions += it
	}
	
	inline operator fun Iterable<String>.invoke(block: Transition.() -> Unit): Unit = Transition().apply(block).let { transition ->
		forEach {
			transitions += transition.copy(property = it)
		}
	}
	
	inline operator fun Array<out String>.invoke(block: Transition.() -> Unit): Unit = Transition().apply(block).let { transition ->
		forEach {
			transitions += transition.copy(property = it)
		}
	}
	
	inline fun properties(vararg properties: String, block: Transition.() -> Unit = {}): Unit = properties.invoke(block)
	
	inline fun all(block: Transition.() -> Unit): Unit = Transition().apply(block).let { transition ->
		transition.property = "all"
		transitions += transition
	}
	
	fun defaultDuration(value: CSSSizeValue<out CSSUnitTime>): Unit = run { defaultDuration = value }
	fun defaultTimingFunction(value: AnimationTimingFunction): Unit = run { defaultTimingFunction = value }
	fun defaultDelay(value: CSSSizeValue<out CSSUnitTime>): Unit = run { defaultDelay = value }
}

@ExperimentalComposeWebApi
/**
 * Examples:
 * ```
 *  transitions {
 *    "width" { duration(1.s) }
 *  }
 *
 *  transitions {
 *    defaultDuration(.5.s)
 *    defaultTimingFunction(AnimationTimingFunction.EaseInOut)
 *    defaultProperties("width", "height")
 *  }
 * ```
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/CSS/transition
 */
inline fun StyleScope.transitions(transitions: Transitions.() -> Unit) {
	val transitionsValue = Transitions().apply(transitions)
	property("transition", transitionsValue.toString())
}