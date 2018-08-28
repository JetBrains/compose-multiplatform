package com.google.r4a.examples.explorerapp.common.api


import android.util.Log
import com.google.gson.*
import com.google.r4a.examples.explorerapp.common.data.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type



/**
 * API communication setup
 */
interface RedditApi {

    @GET("/r/{subreddit}/{filter}.json")
    fun getSubredditLinks(
            @Path("subreddit") subreddit: String,
            @Path("filter") filter: RedditFilterType,
            @Query("limit") limit: Int
    ): Call<ListResponse>

    @GET("/r/{subreddit}/{filter}.json")
    fun getSubredditLinksAfter(
            @Path("subreddit") subreddit: String,
            @Path("filter") filter: RedditFilterType,
            @Query("after") after: String,
            @Query("limit") limit: Int
    ): Call<ListResponse>

    @GET("/r/{subreddit}/{filter}.json")
    fun getSubredditLinksBefore(
            @Path("subreddit") subreddit: String,
            @Path("filter") filter: RedditFilterType,
            @Query("before") before: String,
            @Query("limit") limit: Int
    ): Call<ListResponse>

    @GET("/comments/{link}.json")
    fun getLink(
            @Path("link") link: String,
            @Query("comment") comment: String? = null,
            @Query("limit") limit: Int? = null,
            @Query("depth") depth: Int? = null
    ): Call<LinkResponse>

    @GET("/comments/{link}/{comment}.json")
    fun getComments(
            @Path("link") link: String,
            @Path("comment") comment: String? = null,
            @Query("limit") limit: Int? = null,
            @Query("depth") depth: Int? = null
    ): Call<LinkResponse>

    @FormUrlEncoded
    @POST("/api/morechildren.json")
    fun getMoreChildren(
            @Query("api_type") api_type: String, // "json"
            @Query("link_id") linkId: TypePrefixedId,
            @Field("children") children: String
    ): Call<MoreChildrenResponse>

    companion object {
        private const val BASE_URL = "https://www.reddit.com/"
        fun create(): RedditApi = create(HttpUrl.parse(BASE_URL)!!)
        fun create(httpUrl: HttpUrl): RedditApi {
            val logger = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                Log.d("API", it)
            })
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                    .addInterceptor(logger)
                    .build()
            val gson = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .registerTypeAdapter(RedditObject::class.java, RedditObjectDeserializer())
                    .registerTypeAdapter(LinkResponse::class.java, RedditLinkDeserializer())
                    .create()
            return Retrofit.Builder()
                    .baseUrl(httpUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(RedditApi::class.java)
        }
    }
}

/**
 * A custom deserializer that handles Reddit's serialization strategy, where all objects are "things" that
 * self describe what they are
 */
class RedditObjectDeserializer : JsonDeserializer<RedditObject> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RedditObject? {
        // Sometimes RedditObjects are returned by the API as empty strings. In that case, we just treat
        // it as null
        if (!json.isJsonObject) {
            return null
        }

        try {
            // This object has a "kind" and a "data" attribute that contains the real data. We first
            // deserialize into "ReditObjectWrapper", which will then have the "derived class" of data,
            // which we then deserialize into an instance of that class, and return that directly.
            val wrapper: RedditObjectWrapper = context.deserialize(json, RedditObjectWrapper::class.java)
            return context.deserialize(wrapper.data, wrapper.kind.derivedClass)
        } catch (e: JsonParseException) {
            return null
        }
    }
}


/**
 * Reddit link responses come back as an array of length 2, where the first element of the array is a Link
 * and the second element is a Listing of Comments. Since that's a heterogeneous array, we need to write
 * a custom deserializer for it.
 */
class RedditLinkDeserializer : JsonDeserializer<LinkResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LinkResponse? {
        if (!json.isJsonArray) {
            return null
        }
        try {
            val arr = json.asJsonArray
            return LinkResponse(
                    context.deserialize(arr[0], RedditObject::class.java),
                    context.deserialize(arr[1], RedditObject::class.java)
            )
        } catch (e: JsonParseException) {
            return null
        }
    }
}

