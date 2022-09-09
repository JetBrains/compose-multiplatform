/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AnimationTimingFunction
import org.jetbrains.compose.web.css.s
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
		
		assertEquals("width 1s", nextChild().style.transition)
	}
	
	@Test
	fun multipleProperties() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s) }; "height" { duration(2.s) } }}})
		}
		
		assertEquals("width 1s, height 2s", nextChild().style.transition)
	}
	
	@Test
	fun allProperties() = runTest {
		composition {
			Div({ style { transitions { all { duration(1.s) } }}})
		}
		
		assertEquals("all 1s", nextChild().style.transition)
	}
	
	@Test
	fun ease() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s); ease(AnimationTimingFunction.EaseInOut) }}}})
		}
		
		assertEquals("width 1s ease-in-out", nextChild().style.transition)
	}
	
	@Test
	fun delay() = runTest {
		composition {
			Div({ style { transitions { "width" { duration(1.s); delay(2.s) }}}})
		}
		
		assertEquals("width 1s 2s", nextChild().style.transition)
	}
	
	@Test
	fun properties() = runTest {
		composition {
			Div({ style { transitions { duration(1.s); properties("width", "height") }}})
			Div({ style { transitions { duration(1.s); properties("width, height"); "width" { duration(2.s) }}}})
		}
		
		assertEquals("width 1s, height 1s", nextChild().style.transition)
		assertEquals("width 2s, height 1s", nextChild().style.transition)
	}
}