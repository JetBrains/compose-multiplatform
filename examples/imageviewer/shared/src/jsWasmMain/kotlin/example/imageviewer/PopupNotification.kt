package example.imageviewer

import androidx.compose.runtime.MutableState
import example.imageviewer.view.ToastState

class WebPopupNotification(
    private val toastState: MutableState<ToastState>,
) : PopupNotification() {
    override fun showPopUpMessage(text: String) {
        toastState.value = ToastState.Shown(text)
    }
}