#
# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

cmake_minimum_required(VERSION 3.4.1)

project(compose_ui_inspection)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Werror")

add_library(
        compose_inspection_jni SHARED
        lambda_location_java_jni.cpp)

find_library(android-lib android)
find_library(log-lib log)

# jvmti.h isn't distributed with android ndk, so we need to compile against jdk's one.
# Unfortunately we can't simply use it, because of "include "jni.h"" in it.
# Include with quotes priorities file's directory, so it resolves jdk's jni instead of ndk's one.
# To avoid jdk's jni jvmti.h is copied in another folder.
configure_file($ENV{JAVA_HOME}/include/jvmti.h external_jvmti/jvmti.h COPYONLY)

target_include_directories(compose_inspection_jni
        PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/external_jvmti)
target_link_libraries(compose_inspection_jni ${android-lib} ${log-lib})
