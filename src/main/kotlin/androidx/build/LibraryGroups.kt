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
    val ACTIVITY = LibraryGroup("androidx.activity", LibraryVersions.ACTIVITY)
    val ADS = LibraryGroup("androidx.ads", null)
    val ANNOTATION = LibraryGroup("androidx.annotation", null)
    val APPCOMPAT = LibraryGroup("androidx.appcompat", null)
    val APPSEARCH = LibraryGroup("androidx.appsearch", LibraryVersions.APPSEARCH)
    val ARCH_CORE = LibraryGroup("androidx.arch.core", LibraryVersions.ARCH_CORE)
    val ASYNCLAYOUTINFLATER = LibraryGroup("androidx.asynclayoutinflater",
        LibraryVersions.ASYNCLAYOUTINFLATER)
    val AUTOFILL = LibraryGroup("androidx.autofill", LibraryVersions.AUTOFILL)
    val BIOMETRIC = LibraryGroup("androidx.biometric", LibraryVersions.BIOMETRIC)
    val BROWSER = LibraryGroup("androidx.browser", LibraryVersions.BROWSER)
    val BUILDSRC_TESTS = LibraryGroup("androidx.buildSrc-tests", LibraryVersions.BUILDSRC_TESTS)
    val BENCHMARK = LibraryGroup("androidx.benchmark", LibraryVersions.BENCHMARK)
    val CAMERA = LibraryGroup("androidx.camera", LibraryVersions.CAMERA)
    val CARDVIEW = LibraryGroup("androidx.cardview", LibraryVersions.CARDVIEW)
    val COLLECTION = LibraryGroup("androidx.collection", LibraryVersions.COLLECTION)
    val CONCURRENT = LibraryGroup("androidx.concurrent", LibraryVersions.FUTURES)
    val CONTENTACCESS = LibraryGroup("androidx.contentaccess", LibraryVersions.CONTENTACCESS)
    val CONTENTPAGER = LibraryGroup("androidx.contentpager", LibraryVersions.CONTENTPAGER)
    val COORDINATORLAYOUT = LibraryGroup("androidx.coordinatorlayout",
        LibraryVersions.COORDINATORLAYOUT)
    val COMPOSE = LibraryGroup("androidx.compose", null)
    val CORE = LibraryGroup("androidx.core", null)
    val CURSORADAPTER = LibraryGroup("androidx.cursoradapter", LibraryVersions.CURSORADAPTER)
    val CUSTOMVIEW = LibraryGroup("androidx.customview", LibraryVersions.CUSTOMVIEW)
    val DATASTORE = LibraryGroup("androidx.datastore", LibraryVersions.DATASTORE)
    val DOCUMENTFILE = LibraryGroup("androidx.documentfile", LibraryVersions.DOCUMENTFILE)
    val DRAWERLAYOUT = LibraryGroup("androidx.drawerlayout", LibraryVersions.DRAWERLAYOUT)
    val DYNAMICANIMATION = LibraryGroup("androidx.dynamicanimation", null)
    val EMOJI = LibraryGroup("androidx.emoji", null)
    val ENTERPRISE = LibraryGroup("androidx.enterprise", LibraryVersions.ENTERPRISE)
    val EXIFINTERFACE = LibraryGroup("androidx.exifinterface", LibraryVersions.EXIFINTERFACE)
    val FRAGMENT = LibraryGroup("androidx.fragment", LibraryVersions.FRAGMENT)
    val GRIDLAYOUT = LibraryGroup("androidx.gridlayout", LibraryVersions.GRIDLAYOUT)
    val HEIFWRITER = LibraryGroup("androidx.heifwriter", LibraryVersions.HEIFWRITER)
    val HILT = LibraryGroup("androidx.hilt", null)
    val INSPECTION = LibraryGroup("androidx.inspection", LibraryVersions.INSPECTION)
    val INSPECTION_EXTENSIONS = LibraryGroup("androidx.inspection.extensions",
        LibraryVersions.SQLITE_INSPECTOR)
    val INTERPOLATOR = LibraryGroup("androidx.interpolator", LibraryVersions.INTERPOLATOR)
    val JETIFIER = LibraryGroup("com.android.tools.build.jetifier", null)
    val LEANBACK = LibraryGroup("androidx.leanback", null)
    val LEGACY = LibraryGroup("androidx.legacy", null)
    val LIFECYCLE = LibraryGroup("androidx.lifecycle", null)
    val LOADER = LibraryGroup("androidx.loader", LibraryVersions.LOADER)
    val LOCALBROADCASTMANAGER = LibraryGroup("androidx.localbroadcastmanager",
        LibraryVersions.LOCALBROADCASTMANAGER)
    val MEDIA = LibraryGroup("androidx.media", null)
    val MEDIA2 = LibraryGroup("androidx.media2", LibraryVersions.MEDIA2)
    val MEDIAROUTER = LibraryGroup("androidx.mediarouter", LibraryVersions.MEDIAROUTER)
    val NAVIGATION = LibraryGroup("androidx.navigation", LibraryVersions.NAVIGATION)
    val PAGING = LibraryGroup("androidx.paging", LibraryVersions.PAGING)
    val PALETTE = LibraryGroup("androidx.palette", LibraryVersions.PALETTE)
    val PERCENTLAYOUT = LibraryGroup("androidx.percentlayout", LibraryVersions.PERCENTLAYOUT)
    val PREFERENCE = LibraryGroup("androidx.preference", LibraryVersions.PREFERENCE)
    val PRINT = LibraryGroup("androidx.print", LibraryVersions.PRINT)
    val RECOMMENDATION = LibraryGroup("androidx.recommendation", LibraryVersions.RECOMMENDATION)
    val RECYCLERVIEW = LibraryGroup("androidx.recyclerview", null)
    val REMOTECALLBACK = LibraryGroup("androidx.remotecallback", LibraryVersions.REMOTECALLBACK)
    val ROOM = LibraryGroup("androidx.room", LibraryVersions.ROOM)
    val STARTUP = LibraryGroup("androidx.startup", LibraryVersions.STARTUP)
    val SAVEDSTATE = LibraryGroup("androidx.savedstate", LibraryVersions.SAVEDSTATE)
    val SECURITY = LibraryGroup("androidx.security", null)
    val SERIALIZATION = LibraryGroup("androidx.serialization", LibraryVersions.SERIALIZATION)
    val SHARETARGET = LibraryGroup("androidx.sharetarget", LibraryVersions.SHARETARGET)
    val SLICE = LibraryGroup("androidx.slice", null)
    val SLIDINGPANELAYOUT = LibraryGroup("androidx.slidingpanelayout",
        LibraryVersions.SLIDINGPANELAYOUT)
    val SQLITE = LibraryGroup("androidx.sqlite", LibraryVersions.SQLITE)
    val SWIPEREFRESHLAYOUT = LibraryGroup("androidx.swiperefreshlayout",
        LibraryVersions.SWIPEREFRESHLAYOUT)
    val TESTSCREENSHOT = LibraryGroup("androidx.test.screenshot", LibraryVersions.TESTSCREENSHOT)
    val TEXTCLASSIFIER = LibraryGroup("androidx.textclassifier", LibraryVersions.TEXTCLASSIFIER)
    val TRACING = LibraryGroup("androidx.tracing", LibraryVersions.TRACING)
    val TRANSITION = LibraryGroup("androidx.transition", LibraryVersions.TRANSITION)
    val TVPROVIDER = LibraryGroup("androidx.tvprovider", LibraryVersions.TVPROVIDER)
    val UI = LibraryGroup("androidx.ui", null)
    val VECTORDRAWABLE = LibraryGroup("androidx.vectordrawable", null)
    val VERSIONEDPARCELABLE = LibraryGroup("androidx.versionedparcelable", null)
    val VIEWPAGER = LibraryGroup("androidx.viewpager", LibraryVersions.VIEWPAGER)
    val VIEWPAGER2 = LibraryGroup("androidx.viewpager2", LibraryVersions.VIEWPAGER2)
    val WEAR = LibraryGroup("androidx.wear", LibraryVersions.WEAR)
    val WEBKIT = LibraryGroup("androidx.webkit", LibraryVersions.WEBKIT)
    val WINDOW = LibraryGroup("androidx.window", null)
    val WORK = LibraryGroup("androidx.work", LibraryVersions.WORK)
}

/**
 * This object contains the library group, as well as whether libraries
 * in this group are all required to have the same development version.
 */
data class LibraryGroup(val group: String = "unspecified", val forcedVersion: Version?) {
    val requireSameVersion = (forcedVersion != null)
}
