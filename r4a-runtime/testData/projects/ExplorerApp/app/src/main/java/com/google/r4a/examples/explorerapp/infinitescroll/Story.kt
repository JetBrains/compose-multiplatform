package com.google.r4a.examples.explorerapp.infinitescroll

import android.graphics.Bitmap
import com.restfb.json.JsonObject

class Story(val metadata: JsonObject, val image: Bitmap) {
    val name: String = metadata.getString("name")
    val description: String? = metadata.getString("description").let { if (it == "null") null else it }
}