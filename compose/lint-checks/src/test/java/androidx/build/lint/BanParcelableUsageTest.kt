/*
 * Copyright 2021 The Android Open Source Project
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
class BanParcelableUsageTest : AbstractLintDetectorTest(
    useDetector = BanParcelableUsage(),
    useIssues = listOf(BanParcelableUsage.ISSUE),
) {

    @Test
    fun `Detection of Parcelable usage in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.ParcelableUsageJava"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ParcelableUsageJava.java:25: Error: Class implements android.os.Parcelable [BanParcelableUsage]
public class ParcelableUsageJava implements Parcelable {
             ~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of Parcelable usage in Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.ParcelableUsageKotlin"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ParcelableUsageKotlin.kt:23: Error: Class implements android.os.Parcelable [BanParcelableUsage]
open class ParcelableUsageKotlin protected constructor(parcel: Parcel) : Parcelable {
           ~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }
}