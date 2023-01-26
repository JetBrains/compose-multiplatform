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

package androidx.compose.runtime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupSizeTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun spacerSize() {
        slotExpect(
            "spacerSize",
            noMoreGroupsThan = 8,
            noMoreSlotsThan = 10
        ) {
            Spacer(Modifier)
        }
    }

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun checkboxSize() {
        slotExpect(
            "checkboxSize",
            noMoreGroupsThan = 154,
            noMoreSlotsThan = 179,
        ) {
            Checkbox(true, onCheckedChange = { })
        }
    }

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun textSize() {
        slotExpect(
            "textSize",
            noMoreGroupsThan = 13,
            noMoreSlotsThan = 12
        ) {
            Text("")
        }
    }

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun boxSize() {
        slotExpect(
            "boxSize",
            noMoreGroupsThan = 9,
            noMoreSlotsThan = 10
        ) {
            Box { }
        }
    }

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun buttonSize() {
        slotExpect(
            "buttonSize",
            noMoreGroupsThan = 165,
            noMoreSlotsThan = 193,
        ) {
            androidx.compose.material.Button({ }) {
                Text("Click me")
            }
        }
    }

    @Test
    @MediumTest
    @Ignore("Only run explicitly to check framework")
    fun columnSize() {
        slotExpect(
            "columnSize",
            noMoreGroupsThan = 9,
            noMoreSlotsThan = 13
        ) {
            Column { }
        }
    }

    private fun slotExpect(
        name: String,
        noMoreGroupsThan: Int,
        noMoreSlotsThan: Int,
        content: @Composable () -> Unit
    ) {
        var compositionData: CompositionData? = null
        compose {
            compositionData = currentComposer.compositionData
            currentComposer.disableSourceInformation()
            Marker { content() }
        }.then {
            val group = findMarkerGroup(compositionData!!)
            val receivedGroups = group.groupSize
            val receivedSlots = group.slotsSize

            if (receivedGroups > noMoreGroupsThan || receivedSlots > noMoreSlotsThan) {
                error("Expected $noMoreGroupsThan groups and $noMoreSlotsThan slots " +
                    "but received $receivedGroups and $receivedSlots\n" +
                    "If this was expected execute the gradlew command:\n   ${
                        updateTestCommand(name, receivedGroups, receivedSlots)
                    }"
                )
            }
            if (receivedSlots < noMoreSlotsThan || receivedGroups < noMoreGroupsThan) {
                println(
                    "WARNING: Improvement detected. Update test GroupSizeTests.$name to\n" +
                    "If this was expected, running the gradle command:\n\n" +
                    "   ${updateTestCommand(name, receivedGroups, receivedSlots)}\n\n" +
                    "is recommended"
                )
            }
        }
    }
}

private fun updateTestCommand(name: String, newGroups: Int, newSlots: Int) =
    "./gradlew  -P \"compose.newExpectedSizes=$name,$newGroups,$newSlots\"  " +
        ":compose:runtime:runtime:integration-test:updateExpectedGroupSizes"

private const val MarkerGroup = -441660990

private fun findMarkerGroup(compositionData: CompositionData): CompositionGroup {
    fun findGroup(groups: Iterable<CompositionGroup>, key: Int): CompositionGroup? {
        for (group in groups) {
            if (group.key == key) return group
            findGroup(group.compositionGroups, key)?.let { return it }
        }
        return null
    }

    return findGroup(compositionData.compositionGroups, MarkerGroup)
        ?.compositionGroups
        ?.firstOrNull()
        ?: error("Could not find marker")
}

@Composable
private inline fun Marker(content: @Composable () -> Unit) = content()

// left unused for debugging. This is useful for debugging differences in the slot table
@Suppress("unused")
private fun CompositionGroup.asString(): String {
    fun stringOf(group: CompositionGroup, indent: String): String =
        "$indent ${group.key} ${group.groupSize}:${group.slotsSize}:\n${
            group.compositionGroups.joinToString("") {
                stringOf(it, "$indent  ")
            }}"
    return stringOf(this, "")
}
