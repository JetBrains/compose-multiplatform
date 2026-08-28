/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlin.test.Test
import kotlin.test.assertEquals

// Loads every freshly generated classifier from its JVM facade package.
class GeneratedFacadeClassLoadingTest {
    @Test
    fun allFacadeClassifiersLoadFromTheSafePackage() {
        val report = GeneratedModelReport.read()

        assertEquals(report.counts.getValue("closure"), report.declarations.size)
        report.declarations.forEach { declaration ->
            assertEquals(declaration.commonName, Class.forName(declaration.commonName).name)
        }
    }
}
