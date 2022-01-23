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
Since Compose still use some awt events (https://github.com/JetBrains/compose-jb/issues/1736), you can provide a fake contaniner.

```kotlin
val awtContainer = object : Container() {}

// call it when your custom compose app changes winodw position
fun onWindowUpdate(x: Int, y: Int, width: Int, height: Int) { 
    awtContainer.setBounds(x, y, width, height);
}

CompositionLocalProvider(                                                                                 
    LocalLayerContainer provides awtContainer                   
) {                                                                             
    // Your App                                                                        
}                                                                               
```
