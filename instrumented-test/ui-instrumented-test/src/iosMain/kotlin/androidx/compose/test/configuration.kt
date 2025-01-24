/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test

import kotlinx.cinterop.ExperimentalForeignApi
import androidx.compose.xctest.*
import platform.XCTest.XCTestSuite

@Suppress("unused")
@OptIn(ExperimentalForeignApi::class)
fun testSuite(): XCTestSuite = setupXCTestSuite(
    // Run all test cases from the tests
    // BasicInteractionTest::class,
    // LayersAccessibilityTest::class,

    // Run test cases from a test
    // BasicInteractionTest::testButtonClick,
    // LayersAccessibilityTest::testLayersAppearanceOrder
)
