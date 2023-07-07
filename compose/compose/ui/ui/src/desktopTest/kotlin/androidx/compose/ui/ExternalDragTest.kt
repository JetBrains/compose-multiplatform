/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AwtWindowDragTargetListener.WindowDragValue
import androidx.compose.ui.ExternalDragTest.TestDragEvent.Drag
import androidx.compose.ui.ExternalDragTest.TestDragEvent.DragCancelled
import androidx.compose.ui.ExternalDragTest.TestDragEvent.DragStarted
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.density
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import java.awt.Window
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class ExternalDragTest {
    @Test
    fun `drag inside component that close to top left corner`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<TestDragEvent>()

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Column {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .saveExternalDragEvents(events)
                    )
                }
            }
        }

        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.dragEvents {
            onDragEnterWindow(TestWindowDragValue(Offset(50f, 50f)))
        }
        awaitIdle()
        assertThat(events.size).isEqualTo(1)
        assertThat(events.last()).isEqualTo(DragStarted(Offset(50f, 50f)))

        window.dragEvents {
            onDragInsideWindow(TestWindowDragValue(Offset(70f, 70f)))
        }
        awaitIdle()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.last()).isEqualTo(Drag(Offset(70f, 70f)))
    }


    @Test
    fun `drag enters component that far from top left corner`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<TestDragEvent>()

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window
                Column {
                    Spacer(modifier = Modifier.height(height = 25.dp))
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .saveExternalDragEvents(events)
                    )
                }
            }
        }

        awaitIdle()
        val componentYOffset = with(window.density) {
            25.dp.toPx()
        }

        assertThat(events.size).isEqualTo(0)

        window.dragEvents {
            onDragEnterWindow(TestWindowDragValue(Offset(10f, 10f)))
        }
        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.dragEvents {
            onDragInsideWindow(TestWindowDragValue(Offset(70f, componentYOffset + 1f)))
        }
        awaitIdle()

        assertThat(events.size).isEqualTo(1)
        assertThat(events[0]).isEqualTo(DragStarted(Offset(70f, 1f)))
    }

    @Test
    fun `multiple components`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val eventsComponent1 = mutableListOf<TestDragEvent>()
        val eventsComponent2 = mutableListOf<TestDragEvent>()

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 400.dp, height = 400.dp)
            ) {
                window = this.window
                Column {
                    Box(
                        modifier = Modifier.size(100.dp, 100.dp)
                            .saveExternalDragEvents(eventsComponent1)
                    )
                    Box(
                        modifier = Modifier.size(100.dp, 100.dp)
                            .saveExternalDragEvents(eventsComponent2)
                    )
                }
            }
        }

        awaitIdle()
        val component2YOffset = with(window.density) {
            100.dp.toPx()
        }

        assertThat(eventsComponent1.size).isEqualTo(0)
        assertThat(eventsComponent2.size).isEqualTo(0)

        window.dragEvents {
            onDragEnterWindow(TestWindowDragValue(Offset(10f, 10f)))
        }
        awaitIdle()
        assertThat(eventsComponent1.size).isEqualTo(1)
        assertThat(eventsComponent1.last()).isEqualTo(DragStarted(Offset(10f, 10f)))

        assertThat(eventsComponent2.size).isEqualTo(0)

        window.dragEvents {
            onDragInsideWindow(TestWindowDragValue(Offset(70f, component2YOffset + 1f)))
        }
        awaitIdle()

        assertThat(eventsComponent1.size).isEqualTo(2)
        assertThat(eventsComponent1.last()).isEqualTo(DragCancelled)

        assertThat(eventsComponent2.size).isEqualTo(1)
        assertThat(eventsComponent2.last()).isEqualTo(DragStarted(Offset(70f, 1f)))

        val dragData = createTextDragData("Text")
        window.dragEvents {
            onDrop(TestWindowDragValue(Offset(70f, component2YOffset + 1f), dragData))
        }
        awaitIdle()

        assertThat(eventsComponent1.size).isEqualTo(2)

        assertThat(eventsComponent2.size).isEqualTo(2)
        assertThat(eventsComponent2.last()).isEqualTo(TestDragEvent.Drop(Offset(70f, 1f), dragData))
    }

    @Test
    fun `stop dnd handling when there are no components`() = runApplicationTest {
        lateinit var window: ComposeWindow

        lateinit var componentIsVisible: MutableState<Boolean>

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 400.dp, height = 400.dp)
            ) {
                window = this.window
                Column {
                    componentIsVisible = remember { mutableStateOf(true) }
                    if (componentIsVisible.value) {
                        Box(
                            modifier = Modifier.fillMaxSize().onExternalDrag()
                        )
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window.dropTarget.isActive).isEqualTo(true)

        componentIsVisible.value = false
        awaitIdle()
        assertThat(window.dropTarget.isActive).isEqualTo(false)
    }

    // https://github.com/JetBrains/compose-multiplatform-core/pull/391#discussion_r1128543475
    @Test
    fun `make drag area bigger on hover`() = runApplicationTest {
        lateinit var window: ComposeWindow

        val events = mutableListOf<TestDragEvent>()

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window
                var width by remember { mutableStateOf(50.dp) }
                Column {
                    Box(
                        modifier = Modifier
                            .width(width)
                            .fillMaxHeight()
                            .onExternalDrag(
                                onDragStart = {
                                    // make box bigger on enter
                                    events.add(DragStarted(it.dragPosition))
                                    width = 100.dp
                                },
                                onDragExit = {
                                    // make box smalled when drag exited
                                    events.add(DragCancelled)
                                    width = 50.dp
                                }
                            )
                    )
                }
            }
        }

        awaitIdle()
        assertThat(events.size).isEqualTo(0)

        window.dragEvents {
            onDragEnterWindow(TestWindowDragValue(Offset(25f, 25f)))
        }

        // only one event should be handled -- drag started, even if the component become bigger
        //  since the pointer is always on the component
        repeat(10) {
            awaitIdle()
            assertThat(events.size).isEqualTo(1)
            assertThat(events.last()).isEqualTo(DragStarted(Offset(25f, 25f)))
        }
    }

    private fun Window.dragEvents(eventsProvider: AwtWindowDragTargetListener.() -> Unit) {
        val listener = (dropTarget as AwtWindowDropTarget).dragTargetListener
        listener.eventsProvider()
    }

    @Composable
    private fun Modifier.saveExternalDragEvents(events: MutableList<TestDragEvent>): Modifier {
        return this.onExternalDrag(
            onDragStart = {
                events.add(DragStarted(it.dragPosition, it.dragData))
            },
            onDrop = {
                events.add(TestDragEvent.Drop(it.dragPosition, it.dragData))
            },
            onDrag = {
                events.add(Drag(it.dragPosition, it.dragData))
            },
            onDragExit = {
                events.add(DragCancelled)
            }
        )
    }

    private fun TestWindowDragValue(offset: Offset, dragData: DragData = testDragData): WindowDragValue {
        return WindowDragValue(offset, dragData)
    }

    private sealed interface TestDragEvent {
        data class DragStarted(
            val offset: Offset,
            val dragData: DragData = testDragData
        ) : TestDragEvent

        object DragCancelled : TestDragEvent
        data class Drag(val offset: Offset, val dragData: DragData = testDragData) : TestDragEvent
        data class Drop(val offset: Offset, val dragData: DragData = testDragData) : TestDragEvent
    }

    companion object {
        private val testDragData = createTextDragData("Test text")

        private fun createTextDragData(text: String): DragData {
            return object : DragData.Text {
                override val bestMimeType: String = "text/plain"

                override fun readText(): String {
                    return text
                }
            }
        }
    }
}