/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.jetbrains.compose.components.resources.test.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidResourceStringTest {

    @Test
    fun testStrings_question_mark() {
        getStringResource(R.string.test_question_mark)
    }
    @Test
    fun testStrings_at_mark() {
        getStringResource(R.string.test_at_mark)
    }

    @Test
    fun testStrings_whitespace() {
        getStringResource(R.string.test_whitespace)
    }

    @Test
    fun testString_apostrophe() {
        getStringResource(R.string.test_apostrophe)
    }

    @Test
    fun testString_double_quotation_marks() {
        getStringResource(R.string.test_double_quotation_marks)
    }

    @Test
    fun testString_meld_test() {
        getStringResource(R.string.meld_test)
    }

}
