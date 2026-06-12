An example showing how to integrate Compose with [LWJGL](https://www.lwjgl.org)

Note that:
- the integration is very experimental and can be unstable
- not all features are implemented
- not all features are currently supported (Accessibility, Input Methods)
- to pass some event information it is needed to pass it via AWT events (java.awt.KeyEvent and java.awt.MouseEvent). In the future versions of Compose we plan to get rid of the need of AWT events.



## Problems

### Cursor In TextField
This is easy, you need to provide `androidx.compose.ui.platform.WindowInfo` in CompositionLocal

For example:
```kotlin
CompositionLocalProvider(
    LocalWindowInfo provides object : WindowInfo {  override val isWindowFocused: Boolean = true }
) {
    // Your App
}
```


### Popup
Since Compose still use some AWT events (https://github.com/JetBrains/compose-jb/issues/1736), you can provide a fake contaniner.

```kotlin
val awtContainer = object : Container() {}

// call it when your custom compose app changes window position
fun onWindowUpdate(x: Int, y: Int, width: Int, height: Int) { 
    awtContainer.setBounds(x, y, width, height)
}

CompositionLocalProvider(                                                                                 
    LocalLayerContainer provides awtContainer                   
) {                                                                             
    // Your App                                                                        
}                                                                               
```

`LocalLayerContainer` is internal, so you need to use a trick. 

Create a package `androidx.compose.ui.awt` and create a file `LocalLayerContainer.desktop.kt`

Put following code inside
```kotlin
import androidx.compose.runtime.staticCompositionLocalOf
import your.package.yourFakeAwtContainer

val LocalLayerContainer: androidx.compose.runtime.ProvidableCompositionLocal<java.awt.Container> =
    staticCompositionLocalOf { yourFakeAwtContainer }
```

Then you would be able to access the internal `LocalLayerContainer`
