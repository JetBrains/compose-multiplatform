package com.google.r4a.examples.explorerapp.infinitescroll

import android.content.Context

import com.bumptech.glide.Glide
import com.restfb.json.JsonObject

import org.apache.commons.io.IOUtils

import java.net.URL
import java.util.ArrayList
import java.util.HashSet


/**
 * Simulates a backend data-fetching service, such as a web service, database, etc.
 *
 * This service uses an asynchronous callback when fetching a story, as most scalable data fetching
 * services are asynchronous.
 */
object MyBackendDataApi {
    private var pagesFetched = 0

    private val stories = ArrayList<JsonObject>()
    private val storyIds = HashSet<String>()

    /** Fetch the requested story, returning it via an asynchronous callback  */
    fun getStory(context: Context, storyNumber: Int, callback: Function1<Story, Unit>) {

        // Keep fetching pages of stories until we have the story we need
        if (storyNumber >= stories.size) {
            // TODO(lmr): cant do this with IR
//            synchronized(MyBackendDataApi::class.java) {
                while (storyNumber >= stories.size) fetchNextPage(context)
//            }
        }

        // Return the requested story
        val metadata = stories[storyNumber]
        try {
            Glide
                    .with(context)
                    .load(metadata.getString("image_url"))
                    .asBitmap()
            // TODO(lmr): cant do this with IR
//                    .into(object : SimpleTarget<Bitmap>(500, 500) {
//                        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
//                            if (resource != null) {
//                                callback.invoke(Story(metadata, resource))
//                            }
//                        }
//                    })
        } catch (e: Exception) {
            // TODO(lmr): cant do this with IR
//            throw RuntimeException(e)
        }

        object : Thread() {
            override fun run() {
                // If we're near a page boundary, start prefetching the next page
//                synchronized(MyBackendDataApi::class.java) {
                    while (storyNumber + 5 >= stories.size) fetchNextPage(context)
//                }
            }
        }.start()
    }

    private fun fetchNextPage(context: Context) {
        println("************ Fetching page: " + pagesFetched + " " + stories.size)
        try {
            val page = pagesFetched + 1
            val url = URL("https://api.500px.com/v1/photos?feature=popular&sort=created_at&image_size=5&include_store=store_download&include_states=voted&consumer_key=JTBvG1exrNwwqyXWhM5AKyi8Lb92vpqPf1lsOk61&page=$page")
            val json = IOUtils.toString(url.openStream())
            val array = JsonObject(json).getJsonArray("photos")

            for (i in 0 until array.length()) {
                val metadata = array.getJsonObject(i)
                if (metadata.getBoolean("nsfw")) continue // Skip content that is NotSafeForWork
                if (storyIds.contains(metadata.getString("id"))) continue  // Deduplicate across pages, since ranking can change subtly over time
                stories.add(metadata)
                storyIds.add(metadata.getString("id"))
                Glide
                        .with(context)
                        .load(metadata.getString("image_url"))
                        .downloadOnly(500, 500) // Prefetch
            }
            pagesFetched = page
            println("***************** got the page! " + " " + stories.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
