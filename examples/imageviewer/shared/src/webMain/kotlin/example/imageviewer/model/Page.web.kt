package example.imageviewer.model

actual class MemoryPage actual constructor(actual val pictureIndex: Int) : Page

actual class CameraPage : Page

actual class FullScreenPage actual constructor(actual val pictureIndex: Int) : Page

actual class GalleryPage : Page
