/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support;

/**
 * The list of versions codes of all the libraries in this project.
 */
public class LibraryVersions {
    /**
     * Version code of the support library components.
     */
    public static final Version SUPPORT_LIBRARY = new Version("27.0.0-SNAPSHOT");

    /**
     * Version code for flatfoot 1.0 projects (room, lifecycles)
     */
    private static final Version FLATFOOT_1_0_BATCH = new Version("1.0.0-beta1");

    /**
     * Version code for Room
     */
    public static final Version ROOM = FLATFOOT_1_0_BATCH;

    /**
     * Version code for Lifecycle extensions (live data, view model etc)
     */
    public static final Version LIFECYCLES_EXT = FLATFOOT_1_0_BATCH;

    /**
     * Version code for RecyclerView & Room paging
     */
    public static final Version PAGING = new Version("1.0.0-alpha2");

    /**
     * Version code for Lifecycle libs that are required by the support library
     */
    public static final Version LIFECYCLES_CORE = new Version("1.0.1");

    /**
     * Version code for Lifecycle runtime libs that are required by the support library
     */
    public static final Version LIFECYCLES_RUNTIME = new Version("1.0.0");

    /**
     * Version code for shared code of flatfoot
     */
    public static final Version ARCH_CORE = new Version("1.0.0");

    /**
     * Version code for shared code of flatfoot runtime
     */
    public static final Version ARCH_RUNTIME = FLATFOOT_1_0_BATCH;

    /**
     * Version code for shared testing code of flatfoot
     */
    public static final Version ARCH_CORE_TESTING = FLATFOOT_1_0_BATCH;
}
