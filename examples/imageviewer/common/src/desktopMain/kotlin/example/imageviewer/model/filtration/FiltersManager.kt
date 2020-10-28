package example.imageviewer.model.filtration

import java.awt.image.BufferedImage
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType

class FiltersManager {

    private var filtersMap: MutableMap<FilterType, BitmapFilter> = LinkedHashMap()

    fun clear() {
        filtersMap = LinkedHashMap()
    }

    fun add(filters: Collection<FilterType>) {

        for (filter in filters)
            add(filter)
    }

    fun add(filter: FilterType) {

        if (!filtersMap.containsKey(filter))
            filtersMap[filter] = getFilter(filter)
    }

    fun remove(filter: FilterType) {
        filtersMap.remove(filter)
    }

    fun contains(filter: FilterType): Boolean {
        return filtersMap.contains(filter)
    }

    fun applyFilters(bitmap: BufferedImage): BufferedImage {

        var result: BufferedImage = bitmap
        for (filter in filtersMap) {
            result = filter.value.apply(result)
        }

        return result
    }
}

private fun getFilter(type: FilterType): BitmapFilter {

    return when (type) {
        FilterType.GrayScale -> GrayScaleFilter()
        FilterType.Pixel -> PixelFilter()
        FilterType.Blur -> BlurFilter()
        else -> {
            EmptyFilter()
        }
    }
}