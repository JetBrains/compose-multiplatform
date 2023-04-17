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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertThat
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.isEqualTo
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import java.awt.Point
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import kotlin.test.assertNotEquals
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalMaterialApi::class)
class ApplicationAccessibilityTest {
    @Test
    fun `single component accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Button(
                    onClick = {},
                    modifier = Modifier.size(100.dp)
                ) {
                    Text("Accessible button")
                }
            }
        }
        awaitIdle()

        withAccessibleAt(window, 20, 20) {
            assertThat(accessibleName).isEqualTo("Accessible button")
        }
    }

    @Test
    fun `popup accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                // show popup on top of the accessible button
                val position = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset = IntOffset(0, 25)
                }
                Popup(position, focusable = false) {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(100.dp)
                    ) {
                        Text("Accessible popup button")
                    }
                }
            }
        }
        awaitIdle()

        withAccessibleAt(window, 5, 50) {
            assertThat(accessibleName).isEqualTo("Accessible popup button")
        }
    }

    @Test
    fun `accessibility of multiple components`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 1")
                    }
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("Accessible button 2")
                    }
                }
            }
        }
        awaitIdle()

        withAccessibleAt(window, 10, 10) {
            assertThat(accessibleName).isEqualTo("Accessible button 1")
        }

        withAccessibleAt(window, 5, 22) {
            assertThat(accessibleName).isEqualTo("Accessible button 2")
        }
    }

    // TODO: component under popup shouldn't be read by screen reader
    //  but current implementation does it
    //  (see ComposeSceneAccessible.ComposeSceneAccessibleContext.getAccessibleAt)
    @Ignore
    @Test
    fun `hover popup when there is a component under it`() = runApplicationTest {
        lateinit var window: ComposeWindow

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                Column {
                    Button(
                        onClick = {},
                        modifier = Modifier.size(20.dp),
                    ) {
                        Text("button under popup")
                    }
                    val popupPosition = object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset = IntOffset.Zero
                    }
                    Popup(popupPosition) {
                        Column {
                            Spacer(Modifier.height(30.dp))
                            Button(
                                onClick = {},
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("popup button")
                            }
                        }
                    }
                }
            }
        }
        awaitIdle()

        withAccessibleAt(window, 5, 32) {
            assertThat(accessibleName).isEqualTo("popup button")
        }

        withAccessibleAt(window, 5, 5) {
            assertNotEquals("button under popup", accessibleName)
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/2185
    @Test
    fun `drop-down menu accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var firstItemPositionPx: Offset? = null
        var secondItemPositionPx: Offset? = null

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                DropdownMenu(true, onDismissRequest = {}) {
                    DropdownMenuItem(onClick = {}) {
                        Text("item 1", modifier = Modifier.onGloballyPositioned {
                            firstItemPositionPx = it.positionInWindow()
                        })
                    }
                    DropdownMenuItem(onClick = {}) {
                        Text("item 2", modifier = Modifier.onGloballyPositioned {
                            secondItemPositionPx = it.positionInWindow()
                        })
                    }
                }
            }
        }
        awaitIdle()

        val firstItemPosition = firstItemPositionPx!!.toAwtPoint(window)
        val secondItemPosition = secondItemPositionPx!!.toAwtPoint(window)

        withAccessibleAt(window, firstItemPosition.x + 2, firstItemPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("item 1")
        }

        withAccessibleAt(window, secondItemPosition.x + 2, secondItemPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("item 2")
        }
    }

    // https://github.com/JetBrains/compose-multiplatform/issues/2120
    @Test
    fun `alert dialog accessibility`() = runApplicationTest {
        lateinit var window: ComposeWindow
        var buttonTextPositionPx: Offset? = null
        var textPositionPx: Offset? = null

        launchTestApplication {
            Window(onCloseRequest = {}) {
                window = this.window
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Alert Dialog") },
                    text = {
                        Text(
                            "Alert Dialog Text",
                            modifier = Modifier
                                .onGloballyPositioned { textPositionPx = it.positionInWindow() }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {}) {
                            Text(
                                "Alert Dialog Button",
                                modifier = Modifier
                                    .onGloballyPositioned {
                                        buttonTextPositionPx = it.positionInWindow()
                                    }
                            )
                        }
                    }
                )
            }
        }
        awaitIdle()

        val textPosition = textPositionPx!!.toAwtPoint(window)
        val buttonTextPosition = buttonTextPositionPx!!.toAwtPoint(window)

        withAccessibleAt(window, textPosition.x + 2, textPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("Alert Dialog Text")
        }

        withAccessibleAt(window, buttonTextPosition.x + 2, buttonTextPosition.y + 2) {
            assertThat(accessibleName).isEqualTo("Alert Dialog Button")
        }
    }

    private inline fun withAccessibleAt(
        window: ComposeWindow,
        x: Int,
        y: Int,
        check: AccessibleContext.() -> Unit
    ) {
        val accessibleComponent = window.windowAccessible.accessibleContext as AccessibleComponent
        val accessibleComponentAtPoint = accessibleComponent.getAccessibleAt(Point(x, y))

        check(accessibleComponentAtPoint.accessibleContext)
    }

    private fun Offset.toAwtPoint(window: ComposeWindow): Point = with(window.density) {
        return Point(x.toDp().value.toInt(), y.toDp().value.toInt())
    }
}