/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CameraXQuirksClassDetectorTest : AbstractLintDetectorTest(
    useDetector = CameraXQuirksClassDetector(),
    useIssues = listOf(CameraXQuirksClassDetector.ISSUE)
) {

    @Test
    fun `Detection of CameraX Quirks in Java`() {
        val input = arrayOf(
            javaSample("androidx.CameraXMissingQuirkSummaryJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
            src/androidx/CameraXMissingQuirkSummaryJava.java:22: Error: CameraX quirks should include this template in the javadoc:

            * <p>QuirkSummary
            *     Bug Id:
            *     Description:
            *     Device(s):

             [CameraXQuirksClassDetector]
            public class CameraXMissingQuirkSummaryJava implements Quirk {
                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}