package androidx.compose.ui.layout

/**
 * Interface holding the size and alignment lines of the measured layout, as well as the
 * children positioning logic.
 * [placeChildren] is the function used for positioning children. [Placeable.placeAt] should
 * be called on children inside [placeChildren].
 * The alignment lines can be used by the parent layouts to decide layout, and can be queried
 * using the [Placeable.get] operator. Note that alignment lines will be inherited by parent
 * layouts, such that indirect parents will be able to query them as well.
 */
interface MeasureResult {
    val width: Int
    val height: Int
    val alignmentLines: Map<AlignmentLine, Int>
    fun placeChildren()
}