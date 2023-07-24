package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import example.imageviewer.model.GpsPosition
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LocationVisualizer(
    modifier: Modifier,
    gps: GpsPosition,
    title: String,
    parentScrollEnableState: MutableState<Boolean>
) {
    val location = CLLocationCoordinate2DMake(gps.latitude, gps.longitude)
    val annotation = remember {
        MKPointAnnotation(
            location,
            title = null,
            subtitle = null
        )
    }
    val mkMapView = remember { MKMapView().apply { addAnnotation(annotation) } }
    annotation.setTitle(title)
    UIKitView(
        modifier = modifier,
        factory = {
            mkMapView
        },
        update = {
            mkMapView.setRegion(
                MKCoordinateRegionMakeWithDistance(
                    centerCoordinate = location,
                    10_000.0, 10_000.0
                ),
                animated = false
            )
        }
    )
}
