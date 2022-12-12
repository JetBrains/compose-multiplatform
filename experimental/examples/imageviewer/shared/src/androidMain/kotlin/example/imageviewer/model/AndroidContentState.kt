package example.imageviewer.model

import android.graphics.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.core.createEmptyBitmap
import example.imageviewer.utils.clearCache
import example.imageviewer.utils.isInternetAvailable
import kotlinx.coroutines.*

val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class ContentStateData(
    val filterUIState: Set<FilterType> = emptySet(),
    val isContentReady:Boolean = false,
    val mainImage:Bitmap = createEmptyBitmap(),
    val currentImageIndex:Int = 0,
    val miniatures:Miniatures = Miniatures(),
    val origin: Bitmap? = null,
    val picture:Picture = Picture(image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
)

interface Notification {
    fun notifyInvalidRepo()
    fun notifyRepoIsEmpty()
    fun notifyNoInternet()
    fun notifyLoadImageUnavailable()
    fun notifyLastImage()
    fun notifyFirstImage()
    fun notifyRefreshUnavailable()
}

class ContentState(
    val repository: ImageRepository,
    val getFilter: (FilterType) -> BitmapFilter,
    val state:MutableState<ContentStateData>,
    val notification: Notification,
    val cacheDirProvider: () -> String
) {
    fun getSelectedImage():Bitmap = state.value.mainImage
    fun getMiniatures(): List<Picture> = state.value.miniatures.getMiniatures()
    private val mainImageWrapper = MainImageWrapper(state)

    private fun applyFilters(bitmap: Bitmap):Bitmap {
        var result: Bitmap = bitmap
        for (filter in state.value.filterUIState.map { getFilter(it) }) {
            result = filter.apply(result)
        }
        return result
    }

    fun initData() {
        val directory = cacheDirProvider()
        backgroundScope.launch {
            try {
                if (isInternetAvailable()) {
                    val imageList = repository.get()

                    if (imageList.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            notification.notifyInvalidRepo()
                            onContentReady()
                        }
                        return@launch
                    }

                    val pictureList = loadImages(directory, imageList)

                    if (pictureList.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            notification.notifyRepoIsEmpty()
                            onContentReady()
                        }
                    } else {
                        val picture = loadFullImage(imageList[0])
                        withContext(Dispatchers.Main) {
                            state.value.miniatures.setMiniatures(pictureList)

                            if (isMainImageEmpty()) {
                                wrapPictureIntoMainImage(picture)
                            } else {
                                state.value = state.value.copy(
                                    mainImage = mainImageWrapper.getImage(),
                                    currentImageIndex = mainImageWrapper.getId()
                                )
                            }
                            onContentReady()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        notification.notifyNoInternet()
                        onContentReady()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSelectedImageName(): String {
        return mainImageWrapper.getName()
    }

    private fun toggleFilterState(filter: FilterType) {
        state.value = state.value.copy(
            filterUIState = if (!state.value.filterUIState.contains(filter)) {
                state.value.filterUIState + filter
            } else {
                state.value.filterUIState - filter
            }
        )
    }

    fun toggleFilter(filter: FilterType) {
        toggleFilterState(filter)

        var bitmap = state.value.origin

        if (bitmap != null) {
            bitmap = applyFilters(bitmap)
            mainImageWrapper.setImage(bitmap)
            state.value = state.value.copy(mainImage = bitmap)
        }
    }

    fun isFilterEnabled(type: FilterType): Boolean = state.value.filterUIState.contains(type)

    private fun restoreFilters(): Bitmap {
        state.value = state.value.copy(
            filterUIState = emptySet()
        )
        return mainImageWrapper.restore()
    }

    fun restoreMainImage() {
        state.value = state.value.copy(
            mainImage = restoreFilters()
        )
    }

    // preview/fullscreen image managing
    fun isMainImageEmpty(): Boolean {
        return mainImageWrapper.isEmpty()
    }

    fun fullscreen(picture: Picture) {
        state.value = state.value.copy(isContentReady = false)
        AppState.screenState(ScreenType.FullscreenImage)
        setMainImage(picture)
    }

    fun setMainImage(picture: Picture) {
        if (mainImageWrapper.getId() == picture.id) {
            if (!state.value.isContentReady)
                onContentReady()
            return
        }
        state.value = state.value.copy(isContentReady = false)

        backgroundScope.launch {
            if (isInternetAvailable()) {

                val fullSizePicture = loadFullImage(picture.source).copy(id = picture.id)

                withContext(Dispatchers.Main) {
                    wrapPictureIntoMainImage(fullSizePicture)
                    onContentReady()
                }
            } else {
                withContext(Dispatchers.Main) {
                    notification.notifyLoadImageUnavailable()
                    wrapPictureIntoMainImage(picture)
                }
            }
        }
    }

    private fun onContentReady() {
        state.value = state.value.copy(
            isContentReady = true
        )
    }

    private fun wrapPictureIntoMainImage(picture: Picture) {
        mainImageWrapper.wrapPicture(picture)
        mainImageWrapper.saveOrigin()
        state.value = state.value.copy(
            mainImage = picture.image,
            currentImageIndex = picture.id
        )
    }

    fun swipeNext() {
        if (state.value.currentImageIndex == state.value.miniatures.size() - 1) {
            notification.notifyLastImage()
            return
        }

        restoreFilters()
        val nextIndex = state.value.currentImageIndex + 1
        state.value = state.value.copy(
            currentImageIndex = nextIndex
        )
        setMainImage(state.value.miniatures.get(nextIndex))
    }

    fun swipePrevious() {
        if (state.value.currentImageIndex == 0) {
            notification.notifyFirstImage()
            return
        }

        restoreFilters()
        val prevIndex = state.value.currentImageIndex - 1
        state.value = state.value.copy(
            currentImageIndex = prevIndex
        )
        setMainImage(state.value.miniatures.get(prevIndex))
    }

    fun refresh() {
        backgroundScope.launch {
            if (isInternetAvailable()) {
                withContext(Dispatchers.Main) {
                    clearCache(cacheDirProvider())
                    mainImageWrapper.clear()
                    state.value = state.value.copy(
                        miniatures = Miniatures(),
                        isContentReady = false,
                    )
                    initData()
                }
            } else {
                withContext(Dispatchers.Main) {
                    notification.notifyRefreshUnavailable()
                }
            }
        }
    }

    fun isContentReady(): Boolean = state.value.isContentReady
}

private class MainImageWrapper(val state: MutableState<ContentStateData>) {
    fun saveOrigin() {
        state.value = state.value.copy(
            origin = copy(picture.value.image)
        )
    }

    fun restore(): Bitmap {
        if (state.value.origin != null) {
            filtersSet.clear()
            state.value = state.value.copy(
                picture = state.value.picture.copy(
                    image = copy(state.value.origin!!)
                )
            )
        }
        return copy(picture.value.image)
    }

    // picture adapter
    private var picture = mutableStateOf(
        Picture(image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    )

    fun wrapPicture(picture: Picture) {
        this.picture.value = picture
    }

    fun setImage(bitmap: Bitmap) {
        state.value = state.value.copy(
            picture = state.value.picture.copy(
                image = bitmap
            )
        )
    }

    fun isEmpty(): Boolean {
        return (picture.value.name == "")
    }

    fun clear() {
        picture.value = Picture(image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    fun getName(): String {
        return picture.value.name
    }

    fun getImage(): Bitmap {
        return picture.value.image
    }

    fun getId(): Int {
        return picture.value.id
    }

    // applied filters
    private var filtersSet: MutableSet<FilterType> = LinkedHashSet()

    private fun copy(bitmap: Bitmap): Bitmap {
        return bitmap.copy(bitmap.config, false)
    }
}
