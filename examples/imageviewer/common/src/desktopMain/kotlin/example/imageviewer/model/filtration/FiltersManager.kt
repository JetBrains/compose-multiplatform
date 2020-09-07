/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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