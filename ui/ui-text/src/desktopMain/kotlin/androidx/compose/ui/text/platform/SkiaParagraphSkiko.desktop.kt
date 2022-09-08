package androidx.compose.ui.text.platform

internal actual fun Char.isNeutralDirectionality(): Boolean {
    return this.directionality == CharDirectionality.OTHER_NEUTRALS
        || this.directionality == CharDirectionality.WHITESPACE
        || this.directionality == CharDirectionality.BOUNDARY_NEUTRAL

}