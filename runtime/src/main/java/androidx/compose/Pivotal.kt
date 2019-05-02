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
 * @Pivotal can be applied to the parameters of a composable to indicate that
 * the parameter contributes to the "identity" of the composable.  Pivotal
 * parameters are used when calculating the composable's `key`, and the
 * composable's key is used to determine whether or not the previous results and state of the
 * composable have moved or can be reused.
 *
 * By setting a parameter to a composable function as [Pivotal], you are ensuring that for the
 * life time of that composable in the composition, that parameter will remain unchanged. If it does
 * change (as in, the previous and current values passed into [Object.equals] evaluate to false),
 * then the composable will start a new life time, as if it had been removed and recreated.
 *
 * As a result, the [Pivotal] annotation can be used to simplify component logic, as well as improve
 * performance in some cases.
 *
 * Let's consider the following example, where we have a list of users being displayed from a list
 * of user ids:
 *
 *     @Composable
 *     fun UserList(userIds: List<Int>) {
 *         for (id in userIds) {
 *             UserRow(userId=id)
 *         }
 *     }
 *
 *     @Composable
 *     fun UserRow(userId: Int) {
 *         val user = +state<User?> { null }
 *         +onActive {
 *             val dispose = Api.getUserAsync(userId) { user = it }
 *             onDispose { dispose() }
 *         }
 *         if (user == null) {
 *             LoadingIndicator()
 *             return
 *         }
 *         Image(src=user.profileImage)
 *         Text(text=user.name)
 *     }
 *
 * This example has a bug in it. If the list of user ids is reordered in any way, Compose will reuse
 * previous instances of the UserRow(...) that were created with previous ids. This means that user
 * requests that had previously come in will show up in the wrong position. Semantically, the author
 * of this code had intended `UserRow` to move with the user id, but had not written it to do so.
 *
 * One way to fix this would be to change `onActive` to `onCommit(userId)`:
 *
 *     @Composable
 *     fun UserRow(userId: Int) {
 *         val user = +state<User?> { null }
 *         +onCommit(userId) {
 *             val dispose = Api.getUserAsync(userId) { user = it }
 *             onDispose { dispose() }
 *         }
 *         if (user == null) {
 *             LoadingIndicator()
 *             return
 *         }
 *         Image(src=user.profileImage)
 *         Text(text=user.name)
 *     }
 *
 * In this rendition, the proper users will show up in the proper places, however, in the case where
 * the list of user Ids is shuffled, it is likely that the program will have to execute every API
 * request again, despite not needing to.
 *
 * The reason for this is because despite the intention of the author being that a `UserRow`
 * component's "identity" is determined by which user it is composing (and thus, which userId),
 * Compose has no way of knowing this. The [Pivotal] annotation is meant for exactly this purpose.
 *
 * The ideal and correct implementation of the above `UserRow` component is thus as follows:
 *
 *     @Composable
 *     fun UserRow(@Pivotal userId: Int) {
 *         val user = +state<User?> { null }
 *         +onActive {
 *             val dispose = Api.getUserAsync(userId) { user = it }
 *             onDispose { dispose() }
 *         }
 *         if (user == null) {
 *             LoadingIndicator()
 *             return
 *         }
 *         Image(src=user.profileImage)
 *         Text(text=user.name)
 *     }
 *
 * @see Key
 * @see key
 */
@MustBeDocumented
@Target(
    // composable function paramters
    AnnotationTarget.VALUE_PARAMETER,
    // component class properties
    AnnotationTarget.PROPERTY,
    // component class setter functions
    AnnotationTarget.FUNCTION
)
annotation class Pivotal
