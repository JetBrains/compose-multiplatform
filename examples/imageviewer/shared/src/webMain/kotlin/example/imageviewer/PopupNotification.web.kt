package example.imageviewer

import androidx.compose.runtime.MutableState
import example.imageviewer.view.ToastState
import kotlinx.coroutines.CoroutineScope

class WebPopupNotification(
    private val toastState: MutableState<ToastState>,
    scope: CoroutineScope
) : PopupNotification(scope) {
    override fun showPopUpMessage(text: String) {
        toastState.value = ToastState.Shown(text)
    }
}