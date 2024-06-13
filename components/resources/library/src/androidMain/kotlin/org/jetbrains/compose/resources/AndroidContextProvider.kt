package org.jetbrains.compose.resources

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

internal val androidContext get() = AndroidContextProvider.ANDROID_CONTEXT

//https://andretietz.com/2017/09/06/autoinitialise-android-library/
internal class AndroidContextProvider : ContentProvider() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var ANDROID_CONTEXT: Context
            private set
    }

    override fun onCreate(): Boolean {
        ANDROID_CONTEXT = context ?: error("AndroidContextProvider context is null")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}