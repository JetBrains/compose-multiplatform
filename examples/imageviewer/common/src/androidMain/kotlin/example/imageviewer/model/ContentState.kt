package example.imageviewer.model

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.common.R
import example.imageviewer.core.FilterType
import example.imageviewer.model.filtration.FiltersManager
import example.imageviewer.utils.clearCache
import example.imageviewer.utils.isInternetAvailable
import example.imageviewer.view.showPopUpMessage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object ContentState {

    private lateinit var context: Context
    private lateinit var repository: ImageRepository
    private lateinit var uriRepository: String

    fun applyContent(context: Context, uriRepository: String): ContentState {
        if (this::uriRepository.isInitialized && this.uriRepository == uriRepository) {
            return this
        }

        this.context = context
        this.uriRepository = uriRepository
        repository = ImageRepository(uriRepository)
        appliedFilters = FiltersManager(context)
        isAppUIReady.value = false

        initData()

        return this
    }

    private val executor: ExecutorService by lazy { Executors.newFixedThreadPool(2) }

    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    fun getContext(): Context {
        return context
    }

    fun getOrientation(): Int {
        return context.resources.configuration.orientation
    }

    private val isAppUIReady = mutableStateOf(false)
    fun isContentReady(): Boolean {
        return isAppUIReady.value
    }

    fun getString(id: Int): String {
        return context.getString(id)
    }

    // drawable content
    private val mainImage = mutableStateOf(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val currentImageIndex = mutableStateOf(0)
    private val miniatures = Miniatures()

    fun getMiniatures(): List<Picture> {
        return miniatures.getMiniatures()
    }

    fun getSelectedImage(): Bitmap {
        return mainImage.value
    }

    fun getSelectedImageName(): String {
        return MainImageWrapper.getName()
    }

    // filters managing
    private lateinit var appliedFilters: FiltersManager
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

    private fun restoreFilters(): Bitmap {
        filterUIState.clear()
        appliedFilters.clear()
        return MainImageWrapper.restore()
    }

    fun restoreMainImage() {
        mainImage.value = restoreFilters()
    }

    // application content initialization
    private fun initData() {
        if (isAppUIReady.value)
            return

        val directory = context.cacheDir.absolutePath

        executor.execute {
            try {
                if (isInternetAvailable()) {
                    val imageList = repository.get()

                    if (imageList.isEmpty()) {
                        handler.post {
                            showPopUpMessage(
                                getString(R.string.repo_invalid),
                                context
                            )
                            isAppUIReady.value = true
                        }
                        return@execute
                    }

                    val pictureList = loadImages(directory, imageList)

                    if (pictureList.isEmpty()) {
                        handler.post {
                            showPopUpMessage(
                                getString(R.string.repo_empty),
                                context
                            )
                            isAppUIReady.value = true
                        }
                    } else {
                        val picture = loadFullImage(imageList[0])

                        handler.post {
                            miniatures.setMiniatures(pictureList)

                            if (isMainImageEmpty()) {
                                wrapPictureIntoMainImage(picture)
                            } else {
                                appliedFilters.add(MainImageWrapper.getFilters())
                                mainImage.value = MainImageWrapper.getImage()
                                currentImageIndex.value = MainImageWrapper.getId()
                            }
                            isAppUIReady.value = true
                        }
                    }
                } else {
                    handler.post {
                        showPopUpMessage(
                            getString(R.string.no_internet),
                            context
                        )
                        isAppUIReady.value = true
                    }
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
        isAppUIReady.value = false
        AppState.screenState(ScreenType.FullscreenImage)
        setMainImage(picture)
    }

    fun setMainImage(picture: Picture) {
        if (MainImageWrapper.getId() == picture.id) {
            if (!isContentReady())
                isAppUIReady.value = true
            return
        }

        executor.execute {
            if (isInternetAvailable()) {

                val fullSizePicture = loadFullImage(picture.source)
                fullSizePicture.id = picture.id

                handler.post {
                    wrapPictureIntoMainImage(fullSizePicture)
                    isAppUIReady.value = true
                }
            } else {
                handler.post {
                    showPopUpMessage(
                        "${getString(R.string.no_internet)}\n${getString(R.string.load_image_unavailable)}",
                        context
                    )
                    wrapPictureIntoMainImage(picture)
                }
            }
        }
    }

    private fun wrapPictureIntoMainImage(picture: Picture) {
        MainImageWrapper.wrapPicture(picture)
        MainImageWrapper.saveOrigin()
        mainImage.value = picture.image
        currentImageIndex.value = picture.id
    }

    fun swipeNext() {
        if (currentImageIndex.value == miniatures.size() - 1) {
            showPopUpMessage(
                getString(R.string.last_image),
                context
            )
            return
        }

        restoreFilters()
        setMainImage(miniatures.get(++currentImageIndex.value))
    }

    fun swipePrevious() {
        if (currentImageIndex.value == 0) {
            showPopUpMessage(
                getString(R.string.first_image),
                context
            )
            return
        }

        restoreFilters()
        setMainImage(miniatures.get(--currentImageIndex.value))
    }

    fun refresh() {
        executor.execute {
            if (isInternetAvailable()) {
                handler.post {
                    clearCache(context)
                    miniatures.clear()
                    isAppUIReady.value = false
                    initData()
                }
            } else {
                handler.post {
                    showPopUpMessage(
                        "${getString(R.string.no_internet)}\n${getString(R.string.refresh_unavailable)}",
                        context
                    )
                }
            }
        }
    }
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

    fun addFilter(filter: FilterType) {
        filtersSet.add(filter)
    }

    fun removeFilter(filter: FilterType) {
        filtersSet.remove(filter)
    }

    fun getFilters(): Set<FilterType> {
        return filtersSet
    }

    private fun copy(bitmap: Bitmap): Bitmap {
        return bitmap.copy(bitmap.config, false)
    }
}