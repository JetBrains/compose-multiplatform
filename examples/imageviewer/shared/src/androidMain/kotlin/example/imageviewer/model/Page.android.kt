package example.imageviewer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
actual class MemoryPage actual constructor(actual val pictureIndex: Int) : Page, Parcelable

@Parcelize
actual class CameraPage : Page, Parcelable

@Parcelize
actual class FullScreenPage actual constructor(actual val pictureIndex: Int) : Page, Parcelable

@Parcelize
actual class GalleryPage : Page, Parcelable
