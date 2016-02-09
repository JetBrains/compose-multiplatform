/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.build;

import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defines an API specific support library modules.
 * e.g. Honeycomb implementation of Support-v4.
 *
 * These ApiModules are converted into real modules when project is opened in AndroidStudio.
 * When project is run from the command line, they are converted into source sets.
 * This allows us to rely on previous compile setup to deploy projects with their dependencies while
 * supporting development on Android Studio.
 */
public class ApiModule {
    public static final int CURRENT = 99;
    private String mFolderName;
    private int mApi;
    private SourceSet mSourceSet;
    private ApiModule mPrev;
    private String mParentModuleName;
    private List<String> mParentModuleDependencies;
    private List<String> mResourceFolders = new ArrayList<>();
    private List<String> mAssetFolders = new ArrayList<>();
    private List<String> mJavaResourceFolders = new ArrayList<>();

    public ApiModule(String folderName, int api) {
        mFolderName = folderName;
        mApi = api;
    }

    public ApiModule resources(String... resourceFolders) {
        Collections.addAll(mResourceFolders, resourceFolders);
        return this;
    }

    public ApiModule assets(String... assetFolders) {
        Collections.addAll(mAssetFolders, assetFolders);
        return this;
    }

    public ApiModule javaResources(String... javaResourceFolders) {
        Collections.addAll(mJavaResourceFolders, javaResourceFolders);
        return this;
    }

    public List<String> getResourceFolders() {
        return mResourceFolders;
    }

    public List<String> getAssetFolders() {
        return mAssetFolders;
    }

    public List<String> getJavaResourceFolders() {
        return mJavaResourceFolders;
    }

    public void setResourceFolders(List<String> resourceFolders) {
        mResourceFolders = resourceFolders;
    }

    public String getParentModuleName() {
        return mParentModuleName;
    }

    public void setParentModuleName(String parentModuleName) {
        mParentModuleName = parentModuleName;
    }

    public String getFolderName() {
        return mFolderName;
    }

    public int getApi() {
        return mApi;
    }

    public Object getApiForSourceSet() {
        return mApi == CURRENT ? "current" : mApi;
    }

    public SourceSet getSourceSet() {
        return mSourceSet;
    }

    public void setSourceSet(SourceSet sourceSet) {
        mSourceSet = sourceSet;
    }

    public ApiModule getPrev() {
        return mPrev;
    }

    public void setPrev(ApiModule prev) {
        mPrev = prev;
    }

    public String getModuleName() {
        return ":" + mParentModuleName + "-" + mFolderName;
    }

    public List<String> getParentModuleDependencies() {
        return mParentModuleDependencies;
    }

    public void setParentModuleDependencies(List<String> parentModuleDependencies) {
        mParentModuleDependencies = parentModuleDependencies;
    }
}
