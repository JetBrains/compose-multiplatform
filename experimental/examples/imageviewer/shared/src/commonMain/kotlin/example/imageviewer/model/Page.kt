package example.imageviewer.model

sealed interface Page

class MemoryPage(val picture: PictureData) : Page
//class CameraPage : Page
class FullScreenPage(val picture: PictureData) : Page
class GalleryPage : Page
