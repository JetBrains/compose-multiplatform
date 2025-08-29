package example.imageviewer

import example.imageviewer.filter.PlatformContext
import example.imageviewer.model.PictureData

class WebSharePicture : SharePicture {
    override fun share(context: PlatformContext, picture: PictureData) {
        error("Should not be called")
    }
}
