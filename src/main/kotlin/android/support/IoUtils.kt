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

package android.support

import org.gradle.api.GradleException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

const val TIMEOUT_IN_SECS = 20L //secs

val IO_EXECUTOR: ExecutorService = Executors.newFixedThreadPool(5)
fun <T> ioThread(f: () -> T): Future<T> = IO_EXECUTOR.submit(f)

fun Process.awaitSuccess(processName: String) {
    val awaitResult = waitFor(TIMEOUT_IN_SECS, TimeUnit.SECONDS)
    if (!awaitResult) {
        throw GradleException("Failed to await for $processName")
    }

    if (exitValue() != 0) {
        val errorMessage = errorStream.bufferedReader().use { it.readText() }
        throw GradleException("$processName failed: $errorMessage")
    }
}