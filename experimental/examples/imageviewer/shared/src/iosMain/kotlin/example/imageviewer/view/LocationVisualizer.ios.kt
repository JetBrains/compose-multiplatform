package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitInteropView
import example.imageviewer.model.GpsPosition
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@Composable
internal actual fun LocationVisualizer(modifier: Modifier, gps: GpsPosition) {
    //todo get real geo coordinates
    UIKitInteropView(
        modifier = modifier,
        factory = {
            val mkMapView = MKMapView()
            val cityAmsterdam = CLLocationCoordinate2DMake(gps.latitude, gps.longitude)
            mkMapView.setRegion(
                MKCoordinateRegionMakeWithDistance(
                    centerCoordinate = cityAmsterdam,
                    5000.0, 5000.0
                ),
                animated = false
            )
            mkMapView.addAnnotation(MKPointAnnotation(cityAmsterdam, title = null, subtitle = null))
            mkMapView
        },
    )
}
