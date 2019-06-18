/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

/**
 * [Model] can be applied to a class which represents your application's data model, and will cause
 * instances of the class to become observable, such that a read of a property of an instance of
 * this class during the invocation of a composable function will cause that component to be
 * "subscribed" to mutations of that instance. Composable functions which directly or indirectly
 * read properties of the model class, the composables will be recomposed whenever any properties
 * of the the model are written to.
 *
 * [Model] also adds an MVCC transaction system to ensure data consistency across threads.
 *
 * Consider the following example:
 *
 *     @Model
 *     class LoginState(var username: String, var password: String) {
 *       val valid: Boolean get() = username.length > 0 && password.length > 0
 *       fun login() = Api.login(username, password)
 *     }
 *
 *     @Composable
 *     fun LoginScreen() {
 *       val model = +memo { LoginState() }
 *
 *       EditText(text=model.username, onTextChange={ model.username = it })
 *       EditText(text=model.password, onTextChange={ model.password = it })
 *       Button(text="Login", enabled=model.valid, onPress={ model.login() })
 *     }
 *
 * In this example, `LoginScreen` is recomposed every time the username and password of the
 * model updates, keeping the UI synchronized with the model. There is no need to call `recompose`
 * manually.
 *
 * @see FrameManager
 * @see Observe
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Model
