package example.imageviewer.model

import android.content.Context
import android.graphics.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType
import example.imageviewer.utils.clearCache
import example.imageviewer.utils.isInternetAvailable
import kotlinx.coroutines.*

val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

data class ContentStateData(
    val filterUIState: Set<FilterType> = emptySet(),
    val isContentReady:Boolean = false,
    val mainImage:Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
    val currentImageIndex:Int = 0,
    val miniatures:Miniatures = Miniatures(),
)

interface Notification {
    fun notifyInvalidRepo()
    fun notifyRepoIsEmpty()
    fun notifyNoInternet()
    fun notifyLoadImageUnavaliable()
    fun notifyLastImage()
    fun notifyFirstImage()
    fun notifyRefreshUnavailable()
}

class ContentState(
    val repository: ImageRepository,
    val contextProvider: () -> Context,
    val getFilter: (FilterType) -> BitmapFilter,
    val state:MutableState<ContentStateData>,
    val notification: Notification,
) {
    private val context get() = contextProvider()

    fun getSelectedImage():Bitmap = state.value.mainImage
    fun getMiniatures(): List<Picture> = state.value.miniatures.getMiniatures()

    fun applyFilters(bitmap: Bitmap):Bitmap {
        var result: Bitmap = bitmap
        for (filter in state.value.filterUIState.map { getFilter(it) }) {
            result = filter.apply(result)
        }
        return result
    }

    fun initData() {
        val directory = context.cacheDir.absolutePath
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
                                    mainImage = MainImageWrapper.getImage(),
                                    currentImageIndex = MainImageWrapper.getId()
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
        return MainImageWrapper.getName()
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

        var bitmap = MainImageWrapper.origin

        if (bitmap != null) {
            bitmap = applyFilters(bitmap)
            MainImageWrapper.setImage(bitmap)
            state.value = state.value.copy(mainImage = bitmap)
        }
    }

    fun isFilterEnabled(type: FilterType): Boolean = state.value.filterUIState.contains(type)

    private fun restoreFilters(): Bitmap {
        state.value = state.value.copy(
            filterUIState = emptySet()
        )
        return MainImageWrapper.restore()
    }

    fun restoreMainImage() {
        state.value = state.value.copy(
            mainImage = restoreFilters()
        )
    }

    // preview/fullscreen image managing
    fun isMainImageEmpty(): Boolean {
        return MainImageWrapper.isEmpty()
    }

    fun fullscreen(picture: Picture) {
        state.value = state.value.copy(isContentReady = false)
        AppState.screenState(ScreenType.FullscreenImage)
        setMainImage(picture)
    }

    fun setMainImage(picture: Picture) {
        if (MainImageWrapper.getId() == picture.id) {
            if (!state.value.isContentReady)
                onContentReady()
            return
        }
        state.value = state.value.copy(isContentReady = false)

        backgroundScope.launch {
            if (isInternetAvailable()) {

                val fullSizePicture = loadFullImage(picture.source)
                fullSizePicture.id = picture.id

                withContext(Dispatchers.Main) {
                    wrapPictureIntoMainImage(fullSizePicture)
                    onContentReady()
                }
            } else {
                withContext(Dispatchers.Main) {
                    notification.notifyLoadImageUnavaliable()
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
        MainImageWrapper.wrapPicture(picture)
        MainImageWrapper.saveOrigin()
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
                    clearCache(context)
                    MainImageWrapper.clear()
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

private object MainImageWrapper {
    // origin image
    var origin: Bitmap? = null
        private set

    fun saveOrigin() {
        origin = copy(picture.value.image)
    }

    fun restore(): Bitmap {

        if (origin != null) {
            filtersSet.clear()
            picture.value.image = copy(origin!!)
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
        picture.value.image = bitmap
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
