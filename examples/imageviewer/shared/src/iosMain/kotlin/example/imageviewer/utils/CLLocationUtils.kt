package example.imageviewer.utils

import example.imageviewer.model.GpsPosition
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation

@OptIn(ExperimentalForeignApi::class)
fun CLLocation.toGps() =
    GpsPosition(
        latitude = coordinate.useContents { latitude },
        longitude = coordinate.useContents { longitude }
    )