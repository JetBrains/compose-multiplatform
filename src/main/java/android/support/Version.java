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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class which represents a version
 */
public class Version implements Comparable<Version> {
    private final int mMajor;
    private final int mMinor;
    private final int mPatch;
    private final String mExtra;

    public Version(String versionString) {
        Pattern compile = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(-.+)?$");
        Matcher matcher = compile.matcher(versionString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can not parse version: " + versionString);
        }
        mMajor = Integer.parseInt(matcher.group(1));
        mMinor = Integer.parseInt(matcher.group(2));
        mPatch = Integer.parseInt(matcher.group(3));
        mExtra = matcher.groupCount() == 4 ? matcher.group(4) : null;
    }

    @Override
    public int compareTo(Version version) {
        return mMajor != version.mMajor ? mMajor - version.mMajor : mMinor - version.mMinor;
    }

    public boolean isPatch() {
        return mPatch != 0;
    }

    public boolean isSnapshot() {
        return "-SNAPSHOT".equals(mExtra);
    }

    public int getMajor() {
        return mMajor;
    }

    public int getMinor() {
        return mMinor;
    }

    public int getPatch() {
        return mPatch;
    }

    public String getExtra() {
        return mExtra;
    }
}
