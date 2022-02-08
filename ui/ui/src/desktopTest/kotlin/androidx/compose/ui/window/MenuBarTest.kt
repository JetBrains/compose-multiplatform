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

package androidx.compose.ui.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.readFirstPixel
import androidx.compose.ui.testImage
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.swing.JCheckBoxMenuItem
import javax.swing.JRadioButtonMenuItem
import javax.swing.JSeparator

@OptIn(ExperimentalComposeUiApi::class)
class MenuBarTest {
    @Test(timeout = 20000)
    fun `show and hide menu bar`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isMenubarShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    if (isMenubarShowing) {
                        MenuBar {
                            Menu("Menu0") {
                                Item("Item0", onClick = {})
                                Separator()
                            }
                            Menu("Menu1") {}
                        }
                    }
                }
            }
        }

        awaitIdle()
        assertThat(window?.jMenuBar).isNotNull()
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(2)
            assertThat(getMenu(0).text).isEqualTo("Menu0")
            assertThat(getMenu(1).text).isEqualTo("Menu1")

            getMenu(0).apply {
                assertThat(itemCount).isEqualTo(2)
                assertThat(getItem(0).text).isEqualTo("Item0")
                assertThat(getMenuComponent(1)).isInstanceOf(JSeparator::class.java)
            }

            getMenu(1).apply {
                assertThat(itemCount).isEqualTo(0)
            }
        }

        isMenubarShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar).isNull()

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `show and hide menu`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isMenuShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    MenuBar {
                        if (isMenuShowing) {
                            Menu("Menu0") {
                                Item("Item0", onClick = {})
                                Separator()
                            }
                        }
                        Menu("Menu1") {}
                    }
                }
            }
        }

        awaitIdle()

        isMenuShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar.menuCount).isEqualTo(1)
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(1)
            assertThat(getMenu(0).text).isEqualTo("Menu1")
        }

        isMenuShowing = true
        awaitIdle()
        assertThat(window!!.jMenuBar.menuCount).isEqualTo(2)
        window?.jMenuBar!!.apply {
            assertThat(menuCount).isEqualTo(2)
            assertThat(getMenu(0).text).isEqualTo("Menu0")
            assertThat(getMenu(1).text).isEqualTo("Menu1")
        }

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `show and hide submenu`() = runApplicationTest {
        var isOpen by mutableStateOf(true)
        var isSubmenuShowing by mutableStateOf(true)
        var window: ComposeWindow? = null

        launchApplication {
            if (isOpen) {
                Window(onCloseRequest = {}) {
                    window = this.window

                    MenuBar {
                        Menu("Menu0") {
                            if (isSubmenuShowing) {
                                Menu("Submenu0") {}
                            }
                            Item("Item0", onClick = {})
                            Separator()
                        }
                        Menu("Menu1") {}
                    }
                }
            }
        }

        awaitIdle()

        isSubmenuShowing = false
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).itemCount).isEqualTo(2)
        window?.jMenuBar!!.getMenu(0).apply {
            assertThat(itemCount).isEqualTo(2)
            assertThat(getItem(0).text).isEqualTo("Item0")
            assertThat(getMenuComponent(1)).isInstanceOf(JSeparator::class.java)
        }

        isSubmenuShowing = true
        awaitIdle()
        assertThat(window!!.jMenuBar.getMenu(0).itemCount).isEqualTo(3)
        window?.jMenuBar!!.getMenu(0).apply {
            assertThat(itemCount).isEqualTo(3)
            assertThat(getItem(0).text).isEqualTo("Submenu0")
            assertThat(getItem(1).text).isEqualTo("Item0")
            assertThat(getMenuComponent(2)).isInstanceOf(JSeparator::class.java)
        }

        isOpen = false
    }

    @Test(timeout = 20000)
    fun `change label`() = runApplicationTest {
        var window: ComposeWindow? = null

        var menuLabel by mutableStateOf("Menu")
        var submenuLabel by mutableStateOf("Submenu")
        var itemLabel by mutableStateOf("Item")

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu(menuLabel) {
                        Menu(submenuLabel) {}
                        Item(itemLabel, onClick = {})
                    }
                }
            }
        }

        awaitIdle()
        with(window!!.jMenuBar) {
            assertThat(getMenu(0).text).isEqualTo("Menu")
            assertThat(getMenu(0).getItem(0).text).isEqualTo("Submenu")
            assertThat(getMenu(0).getItem(1).text).isEqualTo("Item")

            menuLabel = "Menu2"
            submenuLabel = "Submenu2"
            itemLabel = "Item2"
            awaitIdle()
            assertThat(getMenu(0).text).isEqualTo("Menu2")
            assertThat(getMenu(0).getItem(0).text).isEqualTo("Submenu2")
            assertThat(getMenu(0).getItem(1).text).isEqualTo("Item2")
        }

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `change enabled`() = runApplicationTest {
        var window: ComposeWindow? = null

        var menuEnabled by mutableStateOf(true)
        var submenuEnabled by mutableStateOf(true)
        var itemEnabled by mutableStateOf(true)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu", enabled = menuEnabled) {
                        Menu("Menu", enabled = submenuEnabled) {}
                        Item("Item", enabled = itemEnabled, onClick = {})
                    }
                }
            }
        }

        awaitIdle()
        with(window!!.jMenuBar) {
            assertThat(getMenu(0).isEnabled).isTrue()
            assertThat(getMenu(0).getItem(0).isEnabled).isTrue()
            assertThat(getMenu(0).getItem(1).isEnabled).isTrue()

            menuEnabled = false
            submenuEnabled = false
            itemEnabled = false
            awaitIdle()
            assertThat(getMenu(0).isEnabled).isFalse()
            assertThat(getMenu(0).getItem(0).isEnabled).isFalse()
            assertThat(getMenu(0).getItem(1).isEnabled).isFalse()
        }

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `change icon`() = runApplicationTest {
        var window: ComposeWindow? = null

        val redIcon = testImage(Color.Red)
        val blueIcon = testImage(Color.Blue)

        var icon: Painter? by mutableStateOf(redIcon)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        Item("Item", icon = icon, onClick = {})
                    }
                }
            }
        }

        val item by lazy { window!!.jMenuBar.getMenu(0).getItem(0) }

        awaitIdle()
        assertThat(item.icon?.readFirstPixel()).isEqualTo(Color.Red)

        icon = blueIcon
        awaitIdle()
        assertThat(item.icon?.readFirstPixel()).isEqualTo(Color.Blue)

        icon = null
        awaitIdle()
        assertThat(item.icon?.readFirstPixel()).isEqualTo(null)

        exitApplication()
    }

    // bug https://github.com/JetBrains/compose-jb/issues/1097#issuecomment-921108560
    @Test(timeout = 20000)
    fun `set icon and disable item`() = runApplicationTest {
        var window: ComposeWindow? = null
        val redIcon = testImage(Color.Red)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        Item("Item", icon = redIcon, enabled = false, onClick = {})
                    }
                }
            }
        }

        awaitIdle()
        window!!.jMenuBar.getMenu(0).doClick()
        window!!.paint(window!!.graphics)

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `change checked of CheckboxItem`() = runApplicationTest {
        var window: ComposeWindow? = null

        var checked by mutableStateOf(true)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        CheckboxItem("Item", checked = checked, onCheckedChange = { checked = it })
                    }
                }
            }
        }

        val item by lazy { window!!.jMenuBar.getMenu(0).getItem(0) as JCheckBoxMenuItem }

        awaitIdle()
        assertThat(item.state).isEqualTo(true)

        checked = false
        awaitIdle()
        assertThat(item.state).isEqualTo(false)

        item.doClick()
        awaitIdle()
        assertThat(checked).isEqualTo(true)
        assertThat(item.state).isEqualTo(true)

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `don't change checked of CheckboxItem`() = runApplicationTest {
        var window: ComposeWindow? = null

        var checked by mutableStateOf(true)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        CheckboxItem("Item", checked = checked, onCheckedChange = { })
                    }
                }
            }
        }

        val item by lazy { window!!.jMenuBar.getMenu(0).getItem(0) as JCheckBoxMenuItem }

        awaitIdle()
        assertThat(item.state).isEqualTo(true)

        checked = false
        awaitIdle()
        assertThat(item.state).isEqualTo(false)

        item.doClick()
        awaitIdle()
        assertThat(checked).isEqualTo(false)
        assertThat(item.state).isEqualTo(false)

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `change status of RadioButtonItem`() = runApplicationTest {
        var window: ComposeWindow? = null

        var selected by mutableStateOf(1)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        RadioButtonItem(
                            "Item0", selected = selected == 0, onClick = { selected = 0 }
                        )
                        RadioButtonItem(
                            "Item1", selected = selected == 1, onClick = { selected = 1 }
                        )
                    }
                }
            }
        }

        val item0 by lazy { window!!.jMenuBar.getMenu(0).getItem(0) as JRadioButtonMenuItem }
        val item1 by lazy { window!!.jMenuBar.getMenu(0).getItem(1) as JRadioButtonMenuItem }

        awaitIdle()
        assertThat(item0.isSelected).isEqualTo(false)
        assertThat(item1.isSelected).isEqualTo(true)

        selected = 0
        awaitIdle()
        assertThat(item0.isSelected).isEqualTo(true)
        assertThat(item1.isSelected).isEqualTo(false)

        item1.doClick()
        awaitIdle()
        assertThat(selected).isEqualTo(1)
        assertThat(item0.isSelected).isEqualTo(false)
        assertThat(item1.isSelected).isEqualTo(true)

        exitApplication()
    }

    @Test(timeout = 20000)
    fun `don't change checked of RadioButtonItem`() = runApplicationTest {
        var window: ComposeWindow? = null

        var selected by mutableStateOf(1)

        launchApplication {
            Window(onCloseRequest = {}) {
                window = this.window

                MenuBar {
                    Menu("Menu") {
                        RadioButtonItem(
                            "Item0", selected = selected == 0, onClick = { }
                        )
                        RadioButtonItem(
                            "Item1", selected = selected == 1, onClick = { }
                        )
                    }
                }
            }
        }

        val item0 by lazy { window!!.jMenuBar.getMenu(0).getItem(0) as JRadioButtonMenuItem }
        val item1 by lazy { window!!.jMenuBar.getMenu(0).getItem(1) as JRadioButtonMenuItem }

        awaitIdle()
        assertThat(item0.isSelected).isEqualTo(false)
        assertThat(item1.isSelected).isEqualTo(true)

        selected = 0
        awaitIdle()
        assertThat(item0.isSelected).isEqualTo(true)
        assertThat(item1.isSelected).isEqualTo(false)

        item1.doClick()
        awaitIdle()
        assertThat(selected).isEqualTo(0)
        assertThat(item0.isSelected).isEqualTo(true)
        assertThat(item1.isSelected).isEqualTo(false)

        exitApplication()
    }
}