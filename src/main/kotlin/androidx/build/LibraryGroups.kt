/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

/**
 * The list of maven group names of all the libraries in this project.
 */
object LibraryGroups {
    val ACTIVITY = LibraryGroup("androidx.activity", false)
    val ANIMATION = LibraryGroup("androidx.animation", false)
    val ANNOTATION = LibraryGroup("androidx.annotation", false)
    val APPCOMPAT = LibraryGroup("androidx.appcompat", false)
    val ARCH_CORE = LibraryGroup("androidx.arch.core", false)
    val ASYNCLAYOUTINFLATER = LibraryGroup("androidx.asynclayoutinflater", false)
    val BIOMETRIC = LibraryGroup("androidx.biometric", false)
    val BROWSER = LibraryGroup("androidx.browser", false)
    val BENCHMARK = LibraryGroup("androidx.benchmark", false)
    val CAMERA = LibraryGroup("androidx.camera", false)
    val CAR = LibraryGroup("androidx.car", false)
    val CARDVIEW = LibraryGroup("androidx.cardview", false)
    val COLLECTION = LibraryGroup("androidx.collection", false)
    val CONCURRENT = LibraryGroup("androidx.concurrent", false)
    val CONTENTPAGER = LibraryGroup("androidx.contentpager", false)
    val COORDINATORLAYOUT = LibraryGroup("androidx.coordinatorlayout", false)
    val CORE = LibraryGroup("androidx.core", false)
    val CURSORADAPTER = LibraryGroup("androidx.cursoradapter", false)
    val CUSTOMVIEW = LibraryGroup("androidx.customview", false)
    val DOCUMENTFILE = LibraryGroup("androidx.documentfile", false)
    val DRAWERLAYOUT = LibraryGroup("androidx.drawerlayout", false)
    val DYNAMICANIMATION = LibraryGroup("androidx.dynamicanimation", false)
    val EMOJI = LibraryGroup("androidx.emoji", false)
    val ENTERPRISE = LibraryGroup("androidx.enterprise", false)
    val EXIFINTERFACE = LibraryGroup("androidx.exifinterface", false)
    val FRAGMENT = LibraryGroup("androidx.fragment", false)
    val GRIDLAYOUT = LibraryGroup("androidx.gridlayout", false)
    val HEIFWRITER = LibraryGroup("androidx.heifwriter", false)
    val INTERPOLATOR = LibraryGroup("androidx.interpolator", false)
    val JETIFIER = LibraryGroup("com.android.tools.build.jetifier", false)
    val LEANBACK = LibraryGroup("androidx.leanback", false)
    val LEGACY = LibraryGroup("androidx.legacy", false)
    val LIFECYCLE = LibraryGroup("androidx.lifecycle", false)
    val LOADER = LibraryGroup("androidx.loader", false)
    val LOCALBROADCASTMANAGER = LibraryGroup("androidx.localbroadcastmanager", false)
    val MEDIA = LibraryGroup("androidx.media", false)
    val MEDIA2 = LibraryGroup("androidx.media2", false)
    val MEDIAROUTER = LibraryGroup("androidx.mediarouter", false)
    val NAVIGATION = LibraryGroup("androidx.navigation", false)
    val PAGING = LibraryGroup("androidx.paging", false)
    val PALETTE = LibraryGroup("androidx.palette", false)
    val PERCENTLAYOUT = LibraryGroup("androidx.percentlayout", false)
    val PERSISTENCE = LibraryGroup("androidx.sqlite", false)
    val PREFERENCE = LibraryGroup("androidx.preference", false)
    val PRINT = LibraryGroup("androidx.print", false)
    val RECOMMENDATION = LibraryGroup("androidx.recommendation", false)
    val RECYCLERVIEW = LibraryGroup("androidx.recyclerview", false)
    val SAVEDSTATE = LibraryGroup("androidx.savedstate", false)
    val SECURITY = LibraryGroup("androidx.security", false)
    val SHARETARGET = LibraryGroup("androidx.sharetarget", false)
    val SLICE = LibraryGroup("androidx.slice", false)
    val REMOTECALLBACK = LibraryGroup("androidx.remotecallback", false)
    val ROOM = LibraryGroup("androidx.room", false)
    val SLIDINGPANELAYOUT = LibraryGroup("androidx.slidingpanelayout", false)
    val SWIPEREFRESHLAYOUT = LibraryGroup("androidx.swiperefreshlayout", false)
    val TEXTCLASSIFIER = LibraryGroup("androidx.textclassifier", false)
    val TRANSITION = LibraryGroup("androidx.transition", false)
    val TVPROVIDER = LibraryGroup("androidx.tvprovider", false)
    val VECTORDRAWABLE = LibraryGroup("androidx.vectordrawable", false)
    val VERSIONEDPARCELABLE = LibraryGroup("androidx.versionedparcelable", false)
    val VIEWPAGER = LibraryGroup("androidx.viewpager", false)
    val VIEWPAGER2 = LibraryGroup("androidx.viewpager2", false)
    val WEAR = LibraryGroup("androidx.wear", false)
    val WEBKIT = LibraryGroup("androidx.webkit", false)
    val WORKMANAGER = LibraryGroup("androidx.work", false)
}

/**
 * This object contains the library group, as well as whether libraries
 * in this group are all required to have the same development version.
 */
data class LibraryGroup(val group: String = "unspecified", val requireSameVersion: Boolean = false)