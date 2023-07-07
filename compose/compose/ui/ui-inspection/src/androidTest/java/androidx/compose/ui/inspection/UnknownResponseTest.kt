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

package androidx.compose.ui.inspection

import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.testdata.TestActivity
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response
import org.junit.Rule
import org.junit.Test

@LargeTest
class UnknownResponseTest {
    @get:Rule
    val rule = ComposeInspectionRule(TestActivity::class)

    @Test
    fun invalidBytesReturnedAsUnknownResponse(): Unit = runBlocking {
        val invalidBytes = (1..99).map { it.toByte() }.toByteArray()
        val responseBytes = rule.inspectorTester.sendCommand(invalidBytes)
        val response = Response.parseFrom(responseBytes)

        assertThat(response.specializedCase)
            .isEqualTo(Response.SpecializedCase.UNKNOWN_COMMAND_RESPONSE)
        assertThat(response.unknownCommandResponse.commandBytes.toByteArray())
            .isEqualTo(invalidBytes)
    }

    @Test
    fun unhandledCommandCaseReturnedAsUnknownResponse(): Unit = runBlocking {
        val invalidCommand = Command.getDefaultInstance()
        // This invalid case is handled by an else branch in ComposeLayoutInspector. In practice,
        // this could also happen when a newer version of Studio sends a new command to an older
        // version of an inspector.
        assertThat(invalidCommand.specializedCase)
            .isEqualTo(Command.SpecializedCase.SPECIALIZED_NOT_SET)

        val commandBytes = invalidCommand.toByteArray()
        val responseBytes = rule.inspectorTester.sendCommand(commandBytes)
        val response = Response.parseFrom(responseBytes)

        assertThat(response.specializedCase)
            .isEqualTo(Response.SpecializedCase.UNKNOWN_COMMAND_RESPONSE)
        assertThat(response.unknownCommandResponse.commandBytes.toByteArray())
            .isEqualTo(commandBytes)
    }
}
