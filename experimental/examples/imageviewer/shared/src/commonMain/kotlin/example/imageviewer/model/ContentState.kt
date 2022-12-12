package example.imageviewer.model

import androidx.compose.runtime.MutableState
import example.imageviewer.core.*
import example.imageviewer.utils.clearCache
import example.imageviewer.utils.isInternetAvailable
import kotlinx.coroutines.*

val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class ContentStateData(
    val filterUIState: Set<FilterType> = emptySet(),
    val isContentReady: Boolean = false,
    val mainImage: CommonBitmap = createEmptyBitmap(),
    val currentImageIndex: Int = 0,
    val miniatures: Miniatures = Miniatures(),
    val origin: CommonBitmap? = null,
    val picture: Picture = Picture(image = createEmptyBitmap()),
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
    val state: MutableState<ContentStateData>,
    val notification: Notification,
    val cacheDirProvider: () -> String
) {
    fun getSelectedImage(): CommonBitmap = state.value.mainImage
    fun getMiniatures(): List<Picture> = state.value.miniatures.getMiniatures()

    private fun applyFilters(bitmap: CommonBitmap): CommonBitmap {
        var result: CommonBitmap = bitmap
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
                                    mainImage = getImage(),
                                    currentImageIndex = getId()
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
        return getName()
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
            setImage(bitmap)
            state.value = state.value.copy(mainImage = bitmap)
        }
    }

    fun isFilterEnabled(type: FilterType): Boolean = state.value.filterUIState.contains(type)

    private fun restoreFilters(): CommonBitmap {
        state.value = state.value.copy(
            filterUIState = emptySet()
        )
        return restore()
    }

    fun restoreMainImage() {
        state.value = state.value.copy(
            mainImage = restoreFilters()
        )
    }

    // preview/fullscreen image managing
    fun isMainImageEmpty(): Boolean {
        return isEmpty()
    }

    fun fullscreen(picture: Picture) {
        state.value = state.value.copy(isContentReady = false)
        AppState.screenState(ScreenType.FullscreenImage)
        setMainImage(picture)
    }

    fun setMainImage(picture: Picture) {
        if (getId() == picture.id) {
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
        wrapPicture(picture)
        saveOrigin()
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
                    clear()
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

    private fun saveOrigin() {
        state.value = state.value.copy(
            origin = copyBitmap(state.value.picture.image)
        )
    }

    private fun restore(): CommonBitmap {
        if (state.value.origin != null) {
            state.value = state.value.copy(
                filterUIState = emptySet(),
                picture = state.value.picture.copy(
                    image = copyBitmap(state.value.origin!!)
                )
            )
        }
        return copyBitmap(state.value.picture.image)
    }

    private fun wrapPicture(picture: Picture) {
        state.value = state.value.copy(
            picture = picture
        )
    }

    private fun setImage(bitmap: CommonBitmap) {
        state.value = state.value.copy(
            picture = state.value.picture.copy(
                image = bitmap
            )
        )
    }

    private fun isEmpty(): Boolean {
        return (state.value.picture.name == "")
    }

    private fun clear() {
        state.value = state.value.copy(
            picture = Picture(image = createEmptyBitmap())
        )
    }

    private fun getName(): String {
        return state.value.picture.name
    }

    private fun getImage(): CommonBitmap {
        return state.value.picture.image
    }

    private fun getId(): Int {
        return state.value.picture.id
    }

    private fun copyBitmap(bitmap: CommonBitmap): CommonBitmap = bitmap.copy()
}
