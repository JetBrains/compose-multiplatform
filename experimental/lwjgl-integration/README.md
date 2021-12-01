An example showing how to integrate Compose with [LWJGL](https://www.lwjgl.org)

Note that:
- the integration is very experimental and can be unstable
- not all features are implemented
- not all features are currently supported (Accessibility, Input Methods)
- to pass some event information it is needed to pass it via AWT events (java.awt.KeyEvent and java.awt.MouseEvent). In the future versions of Compose we plan to get rid of the need of AWT events.
- it has bugs (it doesn't show cursor in TextField)