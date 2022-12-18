//package example.imageviewer.view
//
//import androidx.compose.ui.graphics.ImageBitmap
//import example.imageviewer.core.FilterType
//import example.imageviewer.model.picture
//
//fun toggleFilter(filter: FilterType) {
//    toggleFilterState(filter)
//
//    var bitmap = state.value.origin
//
//    if (bitmap != null) {
//        bitmap = applyFilters(bitmap)
//        setImage(bitmap)
//        state.value = state.value.copy(mainImage = bitmap)
//    }
//}
//
//private fun toggleFilterState(filter: FilterType) {
//    state.value = state.value.copy(
//        filterUIState = if (!state.value.filterUIState.contains(filter)) {
//            state.value.filterUIState + filter
//        } else {
//            state.value.filterUIState - filter
//        }
//    )
//}
//
//fun isFilterEnabled(type: FilterType): Boolean = state.value.filterUIState.contains(type)
//
//private fun restoreFilters(): ImageBitmap {
//    state.value = state.value.copy(
//        filterUIState = emptySet()
//    )
//    return restore()
//}
//
//fun restoreMainImage() {
//    state.value = state.value.copy(
//        mainImage = restoreFilters()
//    )
//}
//
//private fun applyFilters(bitmap: ImageBitmap): ImageBitmap {
//    var result = bitmap
//    for (filter in state.value.filterUIState.map { dependencies.getFilter(it) }) {
//        result = filter.apply(result)
//    }
//    return result
//}
//
//private fun restore(): ImageBitmap {
//    val origin = state.value.origin
//    if (origin != null) {
//        state.value = state.value.copy(
//            filterUIState = emptySet(),
//            picture = state.value.picture?.copy(
//                image = origin
//            )
//        )
//    }
//    return origin!!//todo null check
//}
//
