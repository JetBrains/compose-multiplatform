/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AnimationTimingFunction
import org.jetbrains.compose.web.css.delay
import org.jetbrains.compose.web.css.duration
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.css.timingFunction
import org.jetbrains.compose.web.css.transitions
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalComposeWebApi
class TransitionsTests {
	@Test
	fun duration() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s) } }}})
		}
		
		assertEquals("width 1s ease 0s", nextChild().style.transition)
	}
	
	@Test
	fun multipleProperties() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s) }; "height" { duration(2.s) } }}})
		}
		
		assertEquals("width 1s ease 0s, height 2s ease 0s", nextChild().style.transition)
	}
	
	@Test
	fun allProperties() = runTest {
		composition {
			Div({ style { transitions { all { duration(1.s) } }}})
		}
		
		assertEquals("all 1s ease 0s", nextChild().style.transition)
	}
	
	@Test
	fun timingFunction() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s); timingFunction(AnimationTimingFunction.EaseInOut) }}}})
		}
		
		assertEquals("width 1s ease-in-out 0s", nextChild().style.transition)
	}
	
	@Test
	fun delay() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s); delay(2.s) }}}})
		}
		
		assertEquals("width 1s ease 2s", nextChild().style.transition)
	}
	
	@Test
	fun properties() = runTest {
		composition {
			Div({ style { transitions { defaultDuration(1.s); properties("width", "height") }}})
			Div({ style { transitions { defaultDuration(1.s); properties("width, height"); "width" { duration(2.s) }}}})
			val myList = listOf("width", "height")
			Div({ style { transitions { defaultDuration(1.s); myList { duration(2.s) }}}})
		}
		
		assertEquals("width 1s ease 0s, height 1s ease 0s", nextChild().style.transition)
		assertEquals("width 0s ease 0s, height 1s ease 0s, width 2s ease 0s", nextChild().style.transition)
		assertEquals("width 2s ease 0s, height 2s ease 0s", nextChild().style.transition)
	}
}