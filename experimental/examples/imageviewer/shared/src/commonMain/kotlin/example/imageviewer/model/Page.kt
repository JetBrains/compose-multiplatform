package example.imageviewer.model

import androidx.compose.runtime.MutableState

sealed interface Page

class MemoryPage(val pictureState: MutableState<PictureData>) : Page
class CameraPage : Page
class FullScreenPage(val picture: PictureData) : Page
class GalleryPage : Page
