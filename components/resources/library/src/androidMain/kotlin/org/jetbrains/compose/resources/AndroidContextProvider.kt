package org.jetbrains.compose.resources

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

internal val androidContext get() = AndroidContextProvider.ANDROID_CONTEXT

/**
 * The function configures the android context
 * to be used for non-composable resource read functions
 *
 * e.g. `Res.readBytes(...)`
 *
 * Example usage:
 * ```
 * @Preview
 * @Composable
 * fun MyPreviewComponent() {
 *     PreviewContextConfigurationEffect()
 *     //...
 * }
 * ```
 */
@ExperimentalResourceApi
@Composable
fun PreviewContextConfigurationEffect() {
    if (LocalInspectionMode.current) {
        AndroidContextProvider.ANDROID_CONTEXT = LocalContext.current
    }
}

//https://andretietz.com/2017/09/06/autoinitialise-android-library/
internal class AndroidContextProvider : ContentProvider() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var ANDROID_CONTEXT: Context? = null
    }

    override fun onCreate(): Boolean {
        ANDROID_CONTEXT = context
        return true
    }

    override fun attachInfo(context: Context, info: ProviderInfo?) {
        if (info == null) {
            throw NullPointerException("AndroidContextProvider ProviderInfo cannot be null.")
        }
        // So if the authorities equal the library internal ones, the developer forgot to set his applicationId
        if ("org.jetbrains.compose.components.resources.resources.AndroidContextProvider" == info.authority) {
            throw IllegalStateException("Incorrect provider authority in manifest. Most likely due to a "
                    + "missing applicationId variable your application\'s build.gradle.")
        }

        super.attachInfo(context, info)
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