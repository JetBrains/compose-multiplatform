/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test

import kotlinx.cinterop.ExperimentalForeignApi
import androidx.compose.xctest.*
import platform.XCTest.XCTestSuite

// TODO: create a configuration setup procedure with test selection and reporting
@OptIn(ExperimentalForeignApi::class)
fun testSuite(): XCTestSuite = setupXCTestSuite()
