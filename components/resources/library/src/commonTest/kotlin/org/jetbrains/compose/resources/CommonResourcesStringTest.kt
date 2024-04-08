/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.resources

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalResourceApi::class, InternalResourceApi::class)
class CommonResourceStringTest {
    @Test
    fun testStrings_question_mark() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("test_question_mark"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "test question mark ?")
    }
    @Test
    fun testStrings_at_mark() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("test_at_mark"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "test at mark @")
    }

    @Test
    fun testStrings_whitespace() = runComposeUiTest{
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("test_whitespace"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "test whitespace is \uD83D\uDE0A")
    }

    @Test
    fun testString_apostrophe() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("test_apostrophe"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "test apostrophe it's")
    }

    @Test
    fun testString_double_quotation_marks() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("test_double_quotation_marks"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "\"\uD83D\uDE0A Hello\\'s\n" +
                "        \\@ \\?\n" +
                "        world!\"")
    }

    @Test
    fun testString_meld_test() = runComposeUiTest {
        val testResourceReader = TestResourceReader()
        val res by mutableStateOf(TestStringResource("meld_test"))
        var str = ""
        setContent {
            CompositionLocalProvider(
                LocalResourceReader provides testResourceReader,
                LocalComposeEnvironment provides TestComposeEnvironment
            ) {
                str = stringResource(res)
            }
        }
        val testStrings = handleSpecialCharacters(str)
        assertEquals(testStrings, "\uD83D\uDE0A Hello's @ ? world!")
    }

}
