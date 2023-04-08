package example.imageviewer.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import example.imageviewer.model.GpsPosition

@Composable
actual fun LocationVisualizer(modifier: Modifier, gps: GpsPosition, title: String) {
    var bigMap: Boolean by remember { mutableStateOf(false) }
    Box(modifier) {
        GoogleMapWithMarker(
            gps = gps,
            title = title,
            interactive = false,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable {
            println("bigMap = true")
            bigMap = true
        })
    }
    if (bigMap) {
        Dialog(
            onDismissRequest = { bigMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            GoogleMapWithMarker(
                gps = gps,
                title = title,
                interactive = true,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
fun GoogleMapWithMarker(gps: GpsPosition, title: String, interactive: Boolean, modifier: Modifier) {
    val currentLocation = LatLng(gps.latitude, gps.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 10f)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            compassEnabled = interactive,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = interactive,
            rotationGesturesEnabled = interactive,
            scrollGesturesEnabled = interactive,
            scrollGesturesEnabledDuringRotateOrZoom = interactive,
            tiltGesturesEnabled = interactive,
            zoomControlsEnabled = interactive,
            zoomGesturesEnabled = interactive
        )
    ) {
        Marker(
            state = MarkerState(position = currentLocation),
            title = title,
        )
    }
}
