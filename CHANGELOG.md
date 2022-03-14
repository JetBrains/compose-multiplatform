# 1.1.1 (Mar 2022)
## Desktop

### Fixes
- [Support textIndent](https://github.com/JetBrains/compose-jb/issues/1733)
- [Fix cursor placement inside ligature](https://github.com/JetBrains/compose-jb/issues/1891)

### API changes
- [Compose doesn't depend on kotlinx-coroutines-swing](https://github.com/JetBrains/compose-jb/issues/1943)

If you use `Dispatchers.Swing` or `Dispatchers.Main` in your code, add this dependency into `build.gradle.kts`:
```
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
}
```

Also, usage of `Dispatchers.Swing` or `Dispatchers.Main` inside internals of Compose is implementation details, and can be changed in the future. If you need to avoid race conditions with Compose UI, you can obtain appropriate coroutine scope via `rememberCoroutineScope`:
```
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
fun main() = application {
    val scope = rememberCoroutineScope()
    val someApplicationObject = remember(scope) { SomeApplicationObject(scope) }
    
    DisposableEffect(Unit) {
        SomeGlobalObject.init(scope)
        onDispose {  }
    }
}
```

# 1.1.0 (Feb 2022)
## Desktop

### Features
- [Implement experimental accessibility support for Windows](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Accessibility)
- [Implement accessibility focus tracking by Tab](https://github.com/JetBrains/compose-jb/issues/1772)
- All bugfixes/features between  Jetpack Compose 1.1.0-beta02 and 1.1.0 (see the release notes for each module [here](https://developer.android.com/jetpack/androidx/releases/compose))
- Android target depends on Jetpack Compose 1.1.0.

### Fixes
- Fixes for TextField ([1](https://github.com/JetBrains/compose-jb/issues/1834), [2](https://github.com/JetBrains/compose-jb/issues/1615), [3](https://github.com/JetBrains/compose-jb/issues/1781), [4](https://github.com/JetBrains/compose-jb/issues/1670))
- [Fix exception when we initialize a window with visible = false, undecorated = true](https://github.com/JetBrains/compose-jb/issues/1652)
- [Fix crash in ImageComposeScene](https://github.com/JetBrains/compose-jb/issues/1392)
- [Fixes for situations, when hover state doesn't disappear during scrolling](https://github.com/JetBrains/compose-jb/issues/1324#issuecomment-981148420)
- Fixes for Slider/Scrollbar dragging ([1](https://github.com/JetBrains/compose-jb/issues/643), [2](https://github.com/JetBrains/compose-jb/issues/691))
- [Fixed a case where [event.modifiersEx] does not provide info about the pressed mouse button (AWT)](https://github.com/JetBrains/androidx/pull/181)
- Fix [TextField crashes after selecting the text and then deactivating the text field](https://github.com/JetBrains/compose-jb/issues/1474)
- [Fix consuming events by mouse clickable](https://github.com/JetBrains/androidx/pull/178)
- [Hide top-level dialog from the taskbar](https://github.com/JetBrains/androidx/pull/177)

### API changes
- [The first frame of the window draws offscreen now](https://github.com/JetBrains/compose-jb/issues/1794). If your application has too long start, measure your first frame, and move the heavy logic to background or to the next frames. You can measure the first frame with this snippet:
```
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.singleWindowApplication

private var time by mutableStateOf(System.nanoTime())
private var frame by mutableStateOf(0)

fun main() = singleWindowApplication {
    if (frame == 0) {
        frame++
    } else if (frame == 1) {
        val duration = ((System.nanoTime() - time) / 1E6).toLong()
        println("First frame millis: $duration")
    }
}
```
- [`PointerEvent.awtEvent`, `KeyEvent.awtEvent` are deprecated](https://github.com/JetBrains/androidx/pull/198), use `PointerEvent.awtEventOrNull`, `KeyEvent.awtEventOrNull` instead. The event can be null, if it isn't sent by AWT (for example, Compose can send synthetic Move events on relayout)

# 1.0.1 (Dec 2021)
This is basically 1.0.0 that works with Kotlin 1.6.10

# 1.0.0 (Dec 2021)
## Desktop
### Features
- [Experimental support for material3](https://github.com/JetBrains/compose-jb/issues/1335)
- [All clickable components are focusable and react to Enter key](https://android-review.googlesource.com/c/platform/frameworks/support/+/1858797)
- [All clickable/draggable components no longer react to right clicks by default](https://github.com/JetBrains/compose-jb/issues/832)
- [Show the default error dialog when an error occurs](https://github.com/JetBrains/compose-jb/issues/663)
- [ContextMenu, AlertDialog, DropdownMenu close on Esc key by default](https://github.com/JetBrains/compose-jb/issues/1379)
- [Application by default calls exitProcess after its Composable is disposed](https://github.com/JetBrains/androidx/pull/69)

### API changes
- [Introduced experimental onPointerEvent (will replace mouseScrollFilter/pointerMoveFilter in the future)](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Mouse_Events#mouse-scroll-listeners)
- [Introduced PointerEventType.Scroll, which can be used in common code](https://android-review.googlesource.com/c/platform/frameworks/support/+/1864680)
- [MouseEvent.mouseEvent and KeyEvent.nativeKeyEvent are replaced by MouseEvent.awtEvent and KeyEvent.awtEvent](https://github.com/JetBrains/androidx/pull/87/files)
- [Experimental overload for loading resources painterResource(String, ResourceLoader)](https://github.com/JetBrains/androidx/blob/e2a3108b92d4c54b5780f9eeceb1712845961cd7/compose/ui/ui/src/desktopMain/kotlin/androidx/compose/ui/res/PainterResources.desktop.kt#L64)

## Web
### API changes
- compose.web.widgets is deprecated
- All CSSSelectors can't be instantiated via constructor now (they're private). [Please use functions provided in SelectorsScope instead](https://github.com/JetBrains/compose-jb/blob/master/web/core/src/jsMain/kotlin/org/jetbrains/compose/web/css/StyleSheetBuilder.kt#L37)
- [Some functions were made internal (either not related to compose-web or not intended for usage in apps):  buildCSS , StylePropertyList.nativeEquals , variableValue ,  buildCSSStyleRule , buildKeyframes , jsObject , CSSKeyframesRule.appendRule](https://github.com/JetBrains/compose-jb/pull/1509/files)
- CSSMediaRule: functions feature  and combine  were made extensions functions on GenericStyleSheetBuilder. This makes them consistent with the rest of functions which create CSSMediaQuery.MediaFeature  instances

# 1.0.0-beta (Oct 2021)
## Common
- no Android artifacts are published anymore. Google-published artifacts are referenced instead. This approach eliminates compatibility issues.

## Desktop

### Features
- Accessibility support on MacOS
- Smart rendering fallback logic (no crashes on bad hardware/drivers anymore)
- Performance improvement of software rendering (up to 100%)
- Transparent window support
- `clickable` and `toggleable` components have a hoverable indication

### API changes
- DesktopMaterialTheme is deprecated, use MaterialTheme instead
- WindowSize is deprecated, use DpSize instead
- Modifier.pointerMoveFilter marked as Experimental, stable alternatives are Modifier.hoverable or Modifier.pointerInput
- Modifier.mouseScrollFilter marked as Experimental, this API will likely change in the future

### API breaking changes
- Old Window API (AppWindow, AppManager) was removed
- Modifier.pointerIcon is replaced with Modifier.pointerHoverIcon
- Tray can be used only inside ApplicationScope now
- Tray(hint=) replaced with Tray(tooltip=)

## Web

### Features
- SVG support

### API changes
- Controlled Inputs were added
- New API for testing - test-utils was added 

# 1.0.0-alpha (Aug 2021)
## Common
- Desktop, Web, and Android artifacts publish at the same time with the same version

## Desktop

### Features
- [Context menu support in selectable text](https://android-review.googlesource.com/c/platform/frameworks/support/+/1742314)
- [Cursor change behavior in text and pointer icon API](https://android-review.googlesource.com/c/platform/frameworks/support/+/1736714/12/compose/desktop/desktop/samples/src/jvmMain/kotlin/androidx/compose/desktop/examples/example1/Main.jvm.kt#357)
- [Mouse Clickable modifier](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Mouse_Events#mouse-rightmiddle-clicks-and-keyboard-modifiers)
- Tab navigation between text fields by default
- Resource packing to native distribution
- Support @Preview annotation in desktopMain sourceSet's (when the Compose MPP plugin is installed in IDEA)
- [New features for Composable menu (icons, shortcuts, mnemonics, radiob buttons, checkboxes](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Tray_Notifications_MenuBar_new#menubar)
- [Adaptive window size](https://github.com/JetBrains/compose-jb/blob/master/tutorials/Window_API_new/README.md#adaptive-window-size)
- Support Linux on ARM64
- [Support hidpi on some Linux distros](https://github.com/JetBrains/compose-jb/issues/188#issuecomment-891614869)
- Support resizing of undecorated resizable windows (`Window(undecorated=true, resizable=true, ...)`)

### API changes
- new Window API is no longer experimental
- old Window API is deprecated
- classes from `android.compose.desktop.*` moved to `androidx.compose.ui.awt.*` (ComposeWindow, ComposePanel, etc)
- `svgResource`/`vectorXmlResource`/`imageResource` replaced by painterResource

### API breaking changes
- Window level keyboard API for the old Window API removed
- Window(icon: BufferedImage) replaced by Window(icon: Painter)
- ContextMenu renamed to CursorDropdownMenu

## Web

### API changes
- [classes behave cumulatively](https://github.com/JetBrains/compose-jb/pull/690)
- [removed content builder for empty elements](https://github.com/JetBrains/compose-jb/issues/744)
- [Introduce CSS arithmetic operations](https://github.com/JetBrains/compose-jb/pull/761)
- [Improved the types of Inputs and input events](https://github.com/JetBrains/compose-jb/pull/799)
- [CSS Animations](https://github.com/JetBrains/compose-jb/pull/810)
- [All event types expose native properties](https://github.com/JetBrains/compose-jb/pull/887)
- [Added a complete list of HTML color aliases](https://github.com/JetBrains/compose-jb/issues/890)
- [Introduce support for CSS Grid API](https://github.com/JetBrains/compose-jb/issues/895)
- [Deprecate Color.RGB, Color.HSL etc. functions in favor of top-level rgb, hsl an so on](https://github.com/JetBrains/compose-jb/issues/902)
- [negate CSSNumeric value directly](https://github.com/JetBrains/compose-jb/issues/921)

### API breaking changes
- [boolean like attributes don't have any parameters anymore](https://github.com/JetBrains/compose-jb/pull/780)
- [removed input type specific event listeners](https://github.com/JetBrains/compose-jb/pull/861)
- [replaced maxWidth/minWidth media queries with prefixed names](https://github.com/JetBrains/compose-jb/issues/886)
- [Remove CSSVariables context and introduce specialized methods for adding String- and Number-valued CSS variables](https://github.com/JetBrains/compose-jb/issues/894)
- [inline style builder was moved into AttributeBuilder scope](https://github.com/JetBrains/compose-jb/pull/699)


# M4 (Jun 2021)
  * New experimental [Composable Window API](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Window_API_new)
  * [Tooltips](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Desktop_Components#tooltips)
  * Use [Metal renderer for macOS by default](https://github.com/JetBrains/skiko/pull/70)
  * [Expose a swing mouse event in Modifier.pointerInput](https://github.com/JetBrains/compose-jb/issues/129#issuecomment-784149646)
  * Improved [keyboard support in TextField](https://android-review.googlesource.com/c/platform/frameworks/support/+/1578803)
  * Avoid forcing discrete GPU on multi-GPU MacOS machines in [Skiko](https://github.com/JetBrains/skiko/pull/83) and [native distributions](https://github.com/JetBrains/compose-jb/issues/545)
  * [Make DropdownMenu focusable by default](https://github.com/JetBrains/compose-jb/issues/375)
  * [Scrollbar. get rid of itemCount and averageItemSize from rememberScrollbarAdapte](https://github.com/JetBrains/compose-jb/issues/181)
  * [Support scrollbars for LazyColumn with reverseLayout = true](https://github.com/JetBrains/compose-jb/issues/209)
  * Fix [memory leak](https://github.com/JetBrains/compose-jb/issues/538)
  * Fix [Scroll NaN rounding bug, desktop version](https://github.com/JetBrains/compose-jb/issues/304)
  * Fix [Dragging prevents pointer move events](https://github.com/JetBrains/compose-jb/issues/134)
  * Fix [Dragging window to another display makes Icon show up incorrectly](https://github.com/JetBrains/compose-jb/issues/677)
  * Fix ["Padding must be non-negative" after resizing window with Slider and Box](https://github.com/JetBrains/compose-jb/issues/367)
  * Breaking change [old Dialog/Menubar/Tray are moved to androidx.compose.ui.window.v1](https://android-review.googlesource.com/c/platform/frameworks/support/+/1685905)

# M3 (Feb 2021)
   * Improve [TextField](https://github.com/JetBrains/compose-jb/issues/277)
   * Support [SVG](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Image_And_Icons_Manipulations#loading-svg-images)
   * Support [vsync](https://github.com/JetBrains/skiko/pull/44),  [sync composition with rendering frames](https://android-review.googlesource.com/c/platform/frameworks/support/+/1534675)
   * Support [DirectX on Windows by default](https://github.com/JetBrains/skiko/pull/63)
   * Support [software rendering fallback](https://github.com/JetBrains/skiko/pull/56)
   * Implement [signing and notarization for macOS](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Signing_and_notarization_on_macOS)
   * Improve Swing interoperability support [Swing component in Compose hierarchy](https://github.com/JetBrains/compose-jb/tree/master/tutorials/Swing_Integration#adding-a-swing-component-to-cfd-composition-using-swingpanel)
   * Support using [Compose in IntelliJ plugins](https://github.com/JetBrains/compose-jb/tree/master/examples/intelliJPlugin)
   * Skiko native binaries are now signed on macOS (x64 and arm)
   * Fix [Ambients are not transferred across pop ups](https://github.com/JetBrains/compose-jb/issues/135)
   * Fix [Laggy UI on Linux](https://github.com/JetBrains/compose-jb/issues/23)
   * Fix [Using AndroidX Compose specific dependencies for Android target](https://github.com/JetBrains/compose-jb/issues/272)

# M2 (Dec 2020)
   * Swing interoperability support (Compose in Swing frame)
   * Support [XML vector images](https://developer.android.com/guide/topics/graphics/vector-drawable-resources)
   * [Support for Gradle 6.6 and 6.7](https://github.com/JetBrains/compose-jb/issues/66)
   * [Support macOS 10.13 and 10.14](https://github.com/JetBrains/compose-jb/issues/76)
   * Support Apple Silicon (arm64) natively
   * [Support letter spacing in text](https://github.com/JetBrains/compose-jb/issues/82)
   * [Implemented desktop "ActualDialog" and "ActualPopup"](https://github.com/JetBrains/compose-jb/issues/19)
   * [Fix input method bar positioning](https://github.com/JetBrains/compose-jb/issues/67)
   * [Fix text alignment](https://github.com/JetBrains/compose-jb/issues/92)
   * [Fix dropdown/popup positioning](https://github.com/JetBrains/compose-jb/issues/139)
   * [Fix using with Compose in same app as Java FX](https://github.com/JetBrains/compose-jb/issues/17)
   * [Added screenshots of example apps](https://github.com/JetBrains/compose-jb/issues/90)

# M1 (Nov 2020)
   * Initial release
