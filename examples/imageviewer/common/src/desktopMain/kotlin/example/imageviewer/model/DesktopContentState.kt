package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import example.imageviewer.ResString
import example.imageviewer.core.FilterType
import example.imageviewer.model.filtration.FiltersManager
import example.imageviewer.utils.cacheImagePath
import example.imageviewer.utils.clearCache
import example.imageviewer.utils.isInternetAvailable
import example.imageviewer.view.showPopUpMessage
import example.imageviewer.view.DragHandler
import example.imageviewer.view.ScaleHandler
import example.imageviewer.utils.cropBitmapByScale
import example.imageviewer.utils.toByteArray
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.jetbrains.skija.Image.makeFromEncoded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

object ContentState {
    val drag = DragHandler()
    val scale = ScaleHandler()
    lateinit var windowState: WindowState
    private lateinit var repository: ImageRepository
    private lateinit var uriRepository: String
    val scope = CoroutineScope(Dispatchers.IO)

    fun applyContent(state: WindowState, uriRepository: String): ContentState {
        windowState = state
        if (this::uriRepository.isInitialized && this.uriRepository == uriRepository) {
            return this
        }
        this.uriRepository = uriRepository
        repository = ImageRepository(uriRepository)
        isContentReady.value = false

        initData()

        return this
    }

    private val isAppReady = mutableStateOf(false)
    fun isAppReady(): Boolean {
        return isAppReady.value
    }

    private val isContentReady = mutableStateOf(false)
    fun isContentReady(): Boolean {
        return isContentReady.value
    }

    // drawable content
    private val mainImage = mutableStateOf(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
    private val currentImageIndex = mutableStateOf(0)
    private val miniatures = Miniatures()

    fun getMiniatures(): List<Picture> {
        return miniatures.getMiniatures()
    }

    fun getSelectedImage(): ImageBitmap {
        return MainImageWrapper.mainImageAsImageBitmap.value
    }

    fun getSelectedImageName(): String {
        return MainImageWrapper.getName()
    }

    // filters managing
    private val appliedFilters = FiltersManager()
    private val filterUIState: MutableMap<FilterType, MutableState<Boolean>> = LinkedHashMap()

    private fun toggleFilterState(filter: FilterType) {
        if (!filterUIState.containsKey(filter)) {
            filterUIState[filter] = mutableStateOf(true)
        } else {
            val value = filterUIState[filter]!!.value
            filterUIState[filter]!!.value = !value
        }
    }

    fun toggleFilter(filter: FilterType) {
        if (containsFilter(filter)) {
            removeFilter(filter)
        } else {
            addFilter(filter)
        }

        toggleFilterState(filter)

        var bitmap = MainImageWrapper.origin

        if (bitmap != null) {
            bitmap = appliedFilters.applyFilters(bitmap)
            MainImageWrapper.setImage(bitmap)
            mainImage.value = bitmap
            updateMainImage()
        }
    }

    private fun addFilter(filter: FilterType) {
        appliedFilters.add(filter)
        MainImageWrapper.addFilter(filter)
    }

    private fun removeFilter(filter: FilterType) {
        appliedFilters.remove(filter)
        MainImageWrapper.removeFilter(filter)
    }

    private fun containsFilter(type: FilterType): Boolean {
        return appliedFilters.contains(type)
    }

    fun isFilterEnabled(type: FilterType): Boolean {
        if (!filterUIState.containsKey(type)) {
            filterUIState[type] = mutableStateOf(false)
        }
        return filterUIState[type]!!.value
    }

    private fun restoreFilters(): BufferedImage {
        filterUIState.clear()
        appliedFilters.clear()
        return MainImageWrapper.restore()
    }

    fun restoreMainImage() {
        mainImage.value = restoreFilters()
    }

    // application content initialization
    private fun initData() {
        if (isContentReady.value)
            return

        val directory = File(cacheImagePath)
        if (!directory.exists()) {
            directory.mkdir()
        }

        scope.launch(Dispatchers.IO) {
            try {
                if (isInternetAvailable()) {
                    val imageList = repository.get()

                    if (imageList.isEmpty()) {
                        showPopUpMessage(
                            ResString.repoInvalid
                        )
                        onContentReady()
                    } else {
                        val pictureList = loadImages(cacheImagePath, imageList)

                        if (pictureList.isEmpty()) {
                            showPopUpMessage(
                                ResString.repoEmpty
                            )
                            onContentReady()
                        } else {
                            val picture = loadFullImage(imageList[0])
                            miniatures.setMiniatures(pictureList)
                            if (isMainImageEmpty()) {
                                wrapPictureIntoMainImage(picture)
                            } else {
                                appliedFilters.add(MainImageWrapper.getFilters())
                                currentImageIndex.value = MainImageWrapper.getId()
                            }
                            onContentReady()
                        }
                    }
                } else {
                    showPopUpMessage(
                        ResString.noInternet
                    )
                    onContentReady()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // preview/fullscreen image managing
    fun isMainImageEmpty(): Boolean {
        return MainImageWrapper.isEmpty()
    }

    fun fullscreen(picture: Picture) {
        isContentReady.value = false
        AppState.screenState(ScreenType.FullscreenImage)
        setMainImage(picture)
    }

    fun setMainImage(picture: Picture) {
        if (MainImageWrapper.getId() == picture.id) {
            if (!isContentReady()) {
                onContentReady()
            }
            return
        }
        isContentReady.value = false

        scope.launch(Dispatchers.IO) {
            scale.reset()
            if (isInternetAvailable()) {
                    val fullSizePicture = loadFullImage(picture.source)
                    fullSizePicture.id = picture.id
                    wrapPictureIntoMainImage(fullSizePicture)
            } else {
                    showPopUpMessage(
                        "${ResString.noInternet}\n${ResString.loadImageUnavailable}"
                    )
                    wrapPictureIntoMainImage(picture)
            }
            onContentReady()
        }
    }

    private fun onContentReady() {
        isContentReady.value = true
        isAppReady.value = true
    }

    private fun wrapPictureIntoMainImage(picture: Picture) {
        MainImageWrapper.wrapPicture(picture)
        MainImageWrapper.saveOrigin()
        mainImage.value = picture.image
        currentImageIndex.value = picture.id
        updateMainImage()
    }

    fun updateMainImage() {
        MainImageWrapper.mainImageAsImageBitmap.value = makeFromEncoded(
            toByteArray(
                cropBitmapByScale(
                    mainImage.value,
                    windowState.size,
                    scale.factor.value,
                    drag
                )
            )
        ).asImageBitmap()
    }

    fun swipeNext() {
        if (currentImageIndex.value == miniatures.size() - 1) {
            showPopUpMessage(ResString.lastImage)
            return
        }

        restoreFilters()
        setMainImage(miniatures.get(++currentImageIndex.value))
    }

    fun swipePrevious() {
        if (currentImageIndex.value == 0) {
            showPopUpMessage(ResString.firstImage)
            return
        }

        restoreFilters()
        setMainImage(miniatures.get(--currentImageIndex.value))
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            if (isInternetAvailable()) {
                clearCache()
                MainImageWrapper.clear()
                miniatures.clear()
                isContentReady.value = false
                initData()
            } else {
                showPopUpMessage(
                    "${ResString.noInternet}\n${ResString.refreshUnavailable}"
                )
            }
        }
    }
}

private object MainImageWrapper {
    // origin image
    var origin: BufferedImage? = null
        private set

    fun saveOrigin() {
        origin = copy(picture.value.image)
    }

    fun restore(): BufferedImage {
        if (origin != null) {
            picture.value.image = copy(origin!!)
            filtersSet.clear()
        }
        return copy(picture.value.image)
    }

    var mainImageAsImageBitmap = mutableStateOf(ImageBitmap(1, 1))

    // picture adapter
    private var picture = mutableStateOf(
        Picture(image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
    )

    fun wrapPicture(picture: Picture) {
        this.picture.value = picture
    }

    fun setImage(bitmap: BufferedImage) {
        picture.value.image = bitmap
    }

    fun isEmpty(): Boolean {
        return (picture.value.name == "")
    }

    fun clear() {
        picture.value = Picture(image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
    }

    fun getName(): String {
        return picture.value.name
    }

    fun getImage(): BufferedImage {
        return picture.value.image
    }

    fun getId(): Int {
        return picture.value.id
    }

    // applied filters
    private var filtersSet: MutableSet<FilterType> = LinkedHashSet()

    fun addFilter(filter: FilterType) {
        filtersSet.add(filter)
    }

    fun removeFilter(filter: FilterType) {
        filtersSet.remove(filter)
    }

    fun getFilters(): Set<FilterType> {
        return filtersSet
    }

    private fun copy(bitmap: BufferedImage) : BufferedImage {
        var result = BufferedImage(bitmap.width, bitmap.height, bitmap.type)
        val graphics = result.createGraphics()
        graphics.drawImage(bitmap, 0, 0, result.width, result.height, null)
        return result
    }
}