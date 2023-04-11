package example.imageviewer.model

interface Page

expect class MemoryPage(pictureIndex: Int) : Page {
    val pictureIndex: Int
}

expect class CameraPage() : Page

expect class FullScreenPage(pictureIndex: Int) : Page {
    val pictureIndex: Int
}

expect class GalleryPage() : Page
