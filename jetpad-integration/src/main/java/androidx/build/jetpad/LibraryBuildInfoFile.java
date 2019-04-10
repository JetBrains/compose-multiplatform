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
 */
public class LibraryBuildInfoFile {
  public String version;
  public ArrayList<Dependency> dependencies;
  public ArrayList<Check> checks;

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
