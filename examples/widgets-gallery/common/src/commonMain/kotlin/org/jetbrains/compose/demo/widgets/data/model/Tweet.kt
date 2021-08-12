package org.jetbrains.compose.demo.widgets.data.model

data class Tweet(
    val id: Int,
    val text: String,
    val author: String,
    val handle: String,
    val time: String,
    val authorImageId: String,
    val likesCount: Int,
    val commentsCount: Int,
    val retweetCount: Int,
    val source: String,
    val tweetImageId: String? = null
)