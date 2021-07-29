/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.build.jetpad;

import java.util.ArrayList;

/**
 * Object outlining the format of a library's build info file.
 * This object will be serialized to json.
 * This file should match the corresponding class in Jetpad because
 * this object will be serialized to json and the result will be parsed by Jetpad.
 * DO NOT TOUCH.
 *
 * @property groupId library maven group Id
 * @property artifactId library maven artifact Id
 * @property version library maven version
 * @property path local project directory path used for development, rooted at framework/support
 * @property sha the sha of the latest commit to modify the library (aka a commit that
 * touches a file within [path])
 * @property groupIdRequiresSameVersion boolean that determines if all libraries with [groupId]
 * have the same version
 * @property dependencies a list of dependencies on other androidx libraries
 * @property checks arraylist of [Check]s that is used by Jetpad
 */
public class LibraryBuildInfoFile {
    public String groupId;
    public String artifactId;
    public String version;
    public String kotlinVersion;
    public String path;
    public String sha;
    public String groupZipPath;
    public String projectZipPath;
    public Boolean groupIdRequiresSameVersion;
    public ArrayList<Dependency> dependencies;
    public ArrayList<Check> checks;

    /**
     * @property isTipOfTree boolean that specifies whether the dependency is tip-of-tree
     */
    public class Dependency {
        public String groupId;
        public String artifactId;
        public String version;
        public boolean isTipOfTree;
    }

    public class Check {
        public String name;
        public boolean passing;
    }
}
