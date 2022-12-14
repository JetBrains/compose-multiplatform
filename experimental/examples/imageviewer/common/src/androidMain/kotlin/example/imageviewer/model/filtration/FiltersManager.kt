package example.imageviewer.model.filtration

import android.content.Context
import android.graphics.Bitmap
import example.imageviewer.core.BitmapFilter
import example.imageviewer.core.FilterType

class FiltersManager(private val context: Context) {

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
            filtersMap[filter] = getFilter(filter, context)
    }

    fun remove(filter: FilterType) {
        filtersMap.remove(filter)
    }

    fun contains(filter: FilterType): Boolean {
        return filtersMap.contains(filter)
    }

    fun applyFilters(bitmap: Bitmap): Bitmap {

        var result: Bitmap = bitmap
        for (filter in filtersMap) {
            result = filter.value.apply(result)
        }

        return result
    }
}

private fun getFilter(type: FilterType, context: Context): BitmapFilter {

    return when (type) {
        FilterType.GrayScale -> GrayScaleFilter()
        FilterType.Pixel -> PixelFilter()
        FilterType.Blur -> BlurFilter(context)
    }
}