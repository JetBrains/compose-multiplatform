package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.core.*
import example.imageviewer.utils.isInternetAvailable
import example.imageviewer.utils.ktorHttpClient
import io.ktor.client.request.*
import kotlinx.coroutines.*

val IMAGES_DATA_URL = "https://raw.githubusercontent.com/JetBrains/compose-jb/master/artwork/imageviewerrepo/fetching.list"
val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class ContentStateData(
    val filterUIState: Set<FilterType> = emptySet(),
    val isContentReady: Boolean = false,
    val mainImage: ImageBitmap = createEmptyBitmap(),
    val currentImageIndex: Int = 0,
    val miniatures: Miniatures = Miniatures(),
    val origin: ImageBitmap? = null,
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
    fun notifyImageData(picture: Picture)
}

interface Localization {
    val appName: String // stringResource(R.string.app_name)
    val loading: String // stringResource(R.string.loading)
}

class ContentState(
    val getFilter: (FilterType) -> BitmapFilter,
    val state: MutableState<ContentStateData>,
    val notification: Notification,
    val repository: ContentRepository<NetworkRequest, ImageBitmap>,
    val localization: Localization
) {
    fun getSelectedImage(): ImageBitmap = state.value.mainImage
    fun getMiniatures(): List<Picture> = state.value.miniatures.getMiniatures()

    private fun applyFilters(bitmap: ImageBitmap): ImageBitmap {
        var result = bitmap
        for (filter in state.value.filterUIState.map { getFilter(it) }) {
            result = filter.apply(result)
        }
        return result
    }

    fun initData() {
        backgroundScope.launch {
            try {
                val imageList = ktorHttpClient.get<String>(IMAGES_DATA_URL).lines()

                if (imageList.isEmpty()) {
                    with(notification) {
                        notifyInvalidRepo()
                    }
                    onContentReady()
                    return@launch
                }

                val pictureList: List<Picture> = imageList.map {
                    async {
                        Picture(it, repository.loadContent(NetworkRequest(it)))
                    }
                }.awaitAll()

                if (pictureList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        notification.notifyRepoIsEmpty()
                        onContentReady()
                    }
                } else {
                    val picture = Picture(imageList[0], repository.loadContent(NetworkRequest(imageList[0])))
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    notification.notifyNoInternet()
                    onContentReady()
                }
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

    private fun restoreFilters(): ImageBitmap {
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

                val fullSizePicture = Picture(picture.source, repository.loadContent(NetworkRequest(picture.source)))

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
//                    clearCache(cacheDirProvider())//todo
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
            origin = state.value.picture.image
        )
    }

    private fun restore(): ImageBitmap {
        val origin = state.value.origin
        if (origin != null) {
            state.value = state.value.copy(
                filterUIState = emptySet(),
                picture = state.value.picture.copy(
                    image = origin
                )
            )
        }
        return origin!!//todo null check
    }

    private fun wrapPicture(picture: Picture) {
        state.value = state.value.copy(
            picture = picture
        )
    }

    private fun setImage(bitmap: ImageBitmap) {
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

    private fun getImage(): ImageBitmap {
        return state.value.picture.image
    }

    private fun getId(): Int {
        return state.value.picture.id
    }

}
