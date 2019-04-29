package com.google.r4a

/**
 * [Key] is a utility component that is used to "group" or "key" a block of execution inside of a
 * composition. This is sometimes needed for correctness inside of control-flow that may cause a
 * given composable invocation to execute more than once during composition.
 *
 * The value for a key *does not need to be globally unique*, and needs only be unique amongst the
 * invocations of [Key] *at that point* in composition.
 *
 * For instance, consider the following example:
 *
 *     for (user in users) {
 *         Key(user.id) { UserPreview(user=child) }
 *     }
 *
 *     for (user in users.filter { isAdmin }) {
 *         Key(user.id) { Friend(friend=user) }
 *     }
 *
 * Even though there are users with the same id composed in both the top and the bottom loop,
 * because they are different calls to Key, there is no need to create compound keys.
 *
 * The key must be unique for each element in the collection, however, or children and local state
 * might be reused in unintended ways.
 *
 * For instance, consider the following example:
 *
 *     for ((child, parent) in relationships) {
 *         Key(parent.id) {
 *             User(user=child)
 *             User(user=parent)
*          }
 *     }
 *
 * This example assumes that `parent.id` is a unique key for each item in the collection,
 * but this is only true if it is fair to assume that a parent will only ever have a single child,
 * which may not be the case.  Instead, it may be more correct to do the following:
 *
 *     for ((child, parent) in relationships) {
 *          Key(parent.id to child.id) {
 *             User(user=child)
 *             User(user=parent)
 *          }
 *     }
 *
 *
 * @param key The value used to identify this group. The value will be compared for equality
 *  using [Object.equals] and hashed using [Object.hashCode].
 * @param children The composable children for this group.
 *
 * @see [com.google.r4a.key]
 * @see [Pivotal]
 */
@Composable
@Suppress("PLUGIN_ERROR")
/* inline */
fun Key(@Suppress("UNUSED_PARAMETER") @Pivotal key: Any?, @Children children: () -> Unit) {
    children()
}
