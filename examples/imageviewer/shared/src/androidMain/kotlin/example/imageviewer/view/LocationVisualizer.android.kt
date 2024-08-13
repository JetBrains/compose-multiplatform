package example.imageviewer.view

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import example.imageviewer.model.GpsPosition

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun LocationVisualizer(
    modifier: Modifier,
    gps: GpsPosition,
    title: String,
    parentScrollEnableState: MutableState<Boolean>
) {
    val currentLocation = LatLng(gps.latitude, gps.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 10f)
    }
    LaunchedEffect(cameraPositionState.isMoving) {
        // This code helps to use Compose GoogleMap inside scrollable container.
        // Useful code sample: https://github.com/googlemaps/android-maps-compose/blob/abb3e3581681f26316fdd0b8284597f8fc61daa1/app/src/main/java/com/google/maps/android/compose/MapInColumnActivity.kt#L57
        if (!cameraPositionState.isMoving) {
            parentScrollEnableState.value = true
        }
    }
    GoogleMap(
        modifier = modifier.pointerInteropFilter(
            onTouchEvent = {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        parentScrollEnableState.value = false
                        false
                    }
                    else -> true
                }
            }
        ),
        cameraPositionState = cameraPositionState
    )
}
