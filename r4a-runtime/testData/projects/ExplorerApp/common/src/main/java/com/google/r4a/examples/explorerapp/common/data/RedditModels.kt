package com.google.r4a.examples.explorerapp.common.data

import android.os.Parcelable
import android.support.v7.util.DiffUtil
import com.google.gson.*
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * A Reddit "ID36" string with the type prefix in front, for example "t1_c3v7f8u"
 */
typealias TypePrefixedId = String

/**
 * A Reddit ID36 string without a type prefix, for example "8xwlg"
 */
typealias Id = String

interface Thing {
    val id: Id
    val name: TypePrefixedId
}

interface VotableThing: Thing {
    var ups: Int
    var downs: Int
    var likes: Boolean?
}

interface CreatedThing: Thing {
    val createdUtc: Long
}

interface HierarchicalThing: Thing {
    val depth: Int
    val parentId: TypePrefixedId?
    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<HierarchicalThing>() {
            override fun areContentsTheSame(a: HierarchicalThing, b: HierarchicalThing) = a == b
            override fun areItemsTheSame(a: HierarchicalThing, b: HierarchicalThing) = a.id == b.id
        }
    }
}

enum class AsyncState {
    LOADING,
    DONE,
    FAILED
}

enum class RedditFilterType(private val text: String, val displayText: String) {
    HOT("hot", "Hot Posts"),
    NEW("new", "New Posts"),
    TOP("top", "Top Posts");

    override fun toString() = text
}

data class RedditMore(
        override val name: TypePrefixedId,
        override val id: Id,
        override val parentId: TypePrefixedId?,
        val children: List<Id>
): RedditObject(), HierarchicalThing {
        val count: Int = 0
        override var depth: Int = 0
}

@Parcelize
data class Link(
        override val id: Id,
        override val name: TypePrefixedId,
        val title: String,
        val subredditId: TypePrefixedId,
        val subreddit: String,
        val numComments: Int,
        val author: String,
        val permalink: String,
        val url: String?,
        override val createdUtc: Long,
        val selftext: String,
        val preview: LinkPreview?,
        val thumbnail: String,
        val thumbnailWidth: Int,
        val thumbnailHeight: Int,
        val linkFlairText: String?,
        val authorFlairText: String?,
        val isVideo: Boolean,
        val domain: String,
        val linkFlairTextColor: String?,
        val authorFlairBackgroundColor: String?
): RedditObject(), VotableThing, CreatedThing, Parcelable {
    // since reddit randomizes these, we move them out of the data class constructor to ensure they aren't used
    // in equality checks
    @IgnoredOnParcel
    var score: Int = 0
    @IgnoredOnParcel
    override var likes: Boolean? = null
    @IgnoredOnParcel
    override var ups: Int = 0
    @IgnoredOnParcel
    override var downs: Int = 0

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<Link>() {
            override fun areContentsTheSame(a: Link, b: Link) = a == b
            override fun areItemsTheSame(a: Link, b: Link) = a.name == b.name
        }
    }
}

@Parcelize
data class LinkPreview(
        val images: List<Image>,
        val enabled: Boolean
): Parcelable

@Parcelize
data class Image(
        val id: String,
        val source: ImageSource,
        val resolutions: List<ImageSource>
): Parcelable

@Parcelize
data class ImageSource(
        val url: String,
        val width: Int,
        val height: Int
): Parcelable

@Parcelize
data class Comment(
        override val id: Id,
        override val name: TypePrefixedId,
        val subredditId: TypePrefixedId,
        val linkId: TypePrefixedId,
        val author: String,
        override val parentId: TypePrefixedId?,
        val body: String,
//        val edited: Boolean, // TODO(lmr): This can be false or a date Long
        val isSubmitter: Boolean,
        val subreddit: String,
        val permalink: String,
        val authorFlairText: String?,
        override val createdUtc: Long
): RedditObject(), VotableThing, CreatedThing, HierarchicalThing, Parcelable {

    // it can be a string if empty, so we do this dance. Also, we move it out of the constructor since
    // we want to determine equality by the item but not by its replies
    @IgnoredOnParcel
    var replies: RedditObject? = null
    @IgnoredOnParcel
    override var depth: Int = 0
    @IgnoredOnParcel
    var score: Int = 0
    @IgnoredOnParcel
    override var likes: Boolean? = null
    @IgnoredOnParcel
    override var ups: Int = 0
    @IgnoredOnParcel
    override var downs: Int = 0
    @IgnoredOnParcel
    var isCollapsed: Boolean = false
    @IgnoredOnParcel
    var collapsedChildren: List<HierarchicalThing>? = null

    companion object {
        val COMPARATOR = object : DiffUtil.ItemCallback<Comment>() {
            override fun areContentsTheSame(a: Comment, b: Comment) = a == b
            override fun areItemsTheSame(a: Comment, b: Comment) = a.name == b.name
        }
    }
}

// Below are classes that are really just needed for serialization but not really important semantically
class ListResponse(
        val data: RedditListing
) {
    val links get() = data.children.mapNotNull { it as? Link }
    val after get() = data.after
    val before get() = data.before
}


open class RedditObject

internal data class RedditObjectWrapper(
        val kind: RedditType,
        val data: JsonElement
)

@Suppress("EnumEntryName")
enum class RedditType(val derivedClass: Class<*>) {
    t1(Comment::class.java),
    t3(Link::class.java),
    Listing(RedditListing::class.java),
    more(RedditMore::class.java)
}
class RedditListing(
        val modhash: String,
        val dist: Int,
        val children: List<RedditObject>,
        val after: String?,
        val before: String?
): RedditObject()

class LinkResponse(
        val link: RedditObject,
        val comments: RedditObject
)
class MoreChildrenResponse(val json: Json) {
    class Json(val errors: JsonArray, val data: Data?)
    class Data(val things: List<RedditObject>)
}
