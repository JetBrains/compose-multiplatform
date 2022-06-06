# Movable Content

## Background

Being able to move content within a composition has many advantages. It allows preserving the
internal state of a composition abstractly allowing whole trees to move in the hierarchy. For
example, consider the following code,

```
@Composable
fun MyApplication() {
    if (Mode.current == Mode.Landscape) {
        Row {
           Tile1()
           Tile2()
        }
    } else {
        Column {
           Tile1()
           Tile2()
        }
    }
}
```

If `Mode` changes then all states in `Tile1()` and `Tile2()` are reset including things like scroll
position.  However, if you could treat the composition of the tiles as a unit, such as,

```
@Composable
fun MyApplication() {
    val tiles = remember {
        movableContentOf {
            Tile1()
            Tile2()
        }
    }
    if (Mode.current == Mode.Landscape) {
        Row { tiles() }
    } else {
        Column { tiles() }
   }
}
```

Then the nodes generated for `Tile1()` and `Tile2()` are reused in the composition and any state is
preserved.

Movable content lambdas can also be used in places where a key would be awkward to use or lead to
subtle  cases where state is lost. Consider the following example which splits a collection in two
columns,

```
@Composable
fun <T> NaiveTwoColumns(items: List<T>, composeItem: @Composable (item: T) -> Unit) {
    val half = items.size / 2
    Row {
        Column {
            for (item in items.take(half)) {
                composeItem(item)
            }
        }
        Column {
            for (item in items.drop(half)) {
                composeItem(item)
            }
        }
    }
}

```

This has the same systemic problem all `for` loops have in that the items are used in order of
composition so if data moves around in the collections a lot more recomposition is performed than
is strictly necessary. For example, if one item was inserted at the beginning of the collection the
entire view would need to be recomposed instead of just the first item being created and the rest
being reused unmodified. This can cause the UI to become confused if, for example, input fields with
selection are used in the item block as the selection will not track with the data value but with
the index order in the collection. If the user selected text in the first item, inserting a new item
will cause selection to appear in the new item selecting what appears to be random text and the
selection will be lost in the item the user might have expected to still have selection.

To fix this you can introduce keys such as,

```
@Composable
fun <T> KeyedTwoColumns(items: List<T>, composeItem: @Composable (item: T) -> Unit) {
    val half = items.size / 2
    Row {
        Column {
            for (item in items.take(half)) {
                key(item) {
                    composeItem(item)
                }
            }
        }
        Column {
            for (item in items.drop(half)) {
                key(item) {
                    composeItem(item)
                }
            }
        }
    }
}
```


This allows the composition for an item to be reused if the item is in the same column but discards
the composition, and any implicit state such as selection, and creates a new state. The example
above for the selection in the first item is addressed but if selection is in the last item in a
column then selection is lost entirely as the selection state of the item is discarded.

With `movableContentOf`, the state can be preserved across such movements. For example,

```
@Composable
fun <T> ComposedTwoColumns(items: List<T>, composeItem: @Composable (item: T) -> Unit) {
    val half = items.size / 2
    val composedItems =
        items.map { item -> item to movableContentOf { composeItem(item) } }.toMap()

    Row {
        Column {
            for (item in items.take(half)) {
                composedItems[item]?.invoke()
            }
        }
        Column {
            for (item in items.drop(half)) {
                composedItems[item]?.invoke()
            }
        }
    }
}
```

which maps a movable content lambda to each value in items. This allows the state to be tracked when
the  item moves between columns. This implementation is incorrect as it doesn't preserve the same
instance of the movable content lambda between compositions. This can be corrected (if somewhat
inefficiently) and lifted out into an extension function of `List<T>` to,

```
fun <T> List<T>.movable(
  transform: @Composable (item: T) -> Unit
): @Composable (item: T) -> Unit {
    val composedItems = remember(this) { mutableMapOf<T, () -> Unit>() }
    return { item: T -> composedItems.getOrPut(item) { movableContentOf { transform(item) } } }
}
```

The above allows us to easily reuse `NaiveTwoColumns` with the same state preserving behavior of
`ComposedTwoColumns` by passing in a `composed` lambda such as,
`NaiveTwoColumns(items, items.movable { block(it) })`. This allows a naive layout of items to
become state savvy without modification. Further this allows the state the preservation needs of
the content to be independent of the layout.


## Issue 1: Composition Locals

There are two possibilities for composition locals for a movable content, it has the scope of at the
point of the call to `movableContentOf` that creates it, b) it has the scope of the composition
locals that are in scope where it is placed.

#### Solution chosen: Scope of the placement

In this option the scope of the placement is used instead of the scope of the creator. This leads
to the most natural implementation of movable content being composed lazily at first placement
instead of eagerly. It would also require placing to validate the composition locals of the
placement  and potentially invalidate the composition if the composition locals are different than
expected. To  reduce unused locals from causing an invalidation it might require tracking the usage
of compostion locals and only invalidate if a composition locals it uses is different instead of
just the  scope being different.

Recomposition of an invalid movable content has the same or slightly slower performance than
compositing it from scratch as skipping is disabled as the static composition locals  have changed
and  where they are used is not tracked. This will also affect dynamic composition locals as the
providing an alternative dynamic composition local (such as a font property) is providing a static
composition local of a state object.


* `+` Prevents most eager composition.
* `-` Placing a content can potentially invalidate it, requiring it to be fully recomposed
    (without skipping). To reduce the impact of this might require tracking which composition locals
    are used by the movable content, which is currently not needed.
* `+` Movable content will always use the same ambients as a normal composable lambda.
* `+` Movable content will always be placeable.
* `+` Movable content behave nearly identically to composable functions except for state and
    associated nodes.

#### Alternate considered: Composition locals of the creator

In this option the scope of the creator of the movable content is used in the value. This allows
the content to be composed eagerly and just waiting to be placed.

This, however, could lead to surprising results. For example, if the content was created in one
theme and placed in a different part of the composition with a different theme it would appear out
of  place. Similar effects would be noticeable with fonts as well as the placed text would appear
out  of place. Using the creator's context could also lead to not being able to place the
content at all if the composition locals collide such as having non-interchangeable locals such as
Android `Context`.

Using the creator's scope, however, means that a content can always be placed without recomposition
being required as the only thing that can change are invalidations inside the content itself,
placement will never invalidate it.

*   `+` Allows for eager composition of the content.
*   `+` Content can be placed without causing them to be invalidated.
*   `-` Content will appear out of context when the ambient scope affects its visual appearance.
*   `-` Content can become unplaceable if the composition locals are incompatible with the
        placement locals. This would be difficult to detect and might require modification of the
        composition local API to allow such detection.
*   `-` Content behavior when placed differs significantly from a normal composable function.

## Issue 2: Multiple placements

Once a movable content lambda has been created there is very little that can be done to prevent the
developer from placing the content multiple times in the same outer composition. The options
are to a) throw an exception on the second placement of the content, b) ignore the placement of any
placement except the first or last, c) treat subsequent placements as valid and create as many
copies as needed to fulfill the placements used, d) reference the same state from every invocation.

#### The solution chosen: Create copies as needed

With this option a movable content acts very similar to a key where it is not necessary for them
to be unique. In composition, if a key value is used more than once then composition order is used
to disambiguate the compositions associated with the key. Similarly, if movable content is used
more than once, the composition function whose state is being tracked is just called again to
produce a duplicate state. Instead of solely using composition order (which might cause the state
to move unexpectedly), a new state will only be created for invocations that were not present in
the previous composition and order of composition will be used for movable content whose source
locations change. In other words, movable content will only move its state if the lambda was
not called in the same location in the composition as it was called in the previous composition.
This is not true of keys as the first use of a key might steal the state of a key that was
generated in the same location it was previously.

*   `-` Complex to implement as the state values need to be tracked through the entire composition
    and many of the decisions must be resolved after all other composition has been completed.
*   `+` Movable content lambdas behave like normal composition functions except the state moves with
    their invocation. This allows movable content lambdas and normal composition lambda to be
    interchangeable.
*   `-` Composition can occur out of the order that the developer might expect as recomposition of
    movable content might occur after all other recomposition has completed.
*   `-` If movable content is placed indirectly, such as might happen when using it in
    `Crossfade`, the state will not be shared as the new state will be created for the new
    invocation and the invocation that retains the state will eventually be removed. It is
    difficult to predict how a child will use a movable content or how to influence its use
    of the content to retain the state as desired.
*   `-` In the "list recycling" case this can lead to surprising behavior like selection not being
    reset during recycling _sometimes_.


#### Alternate (A) considered: Throw an exception

With this option placing movable content more than once would cause an exception to be thrown
on the second placement.

*   `+` Simpler to implement as no additional tracking of content is necessary. It just
        needs to detect the second placement.
*   `-` Movable content need careful handling when placing to ensure that they are not
        accidentally placed more than once.
*   `-` Movable content lambdas behave differently than normal composable lambda in that they can
        only be called once.
*   `-` Ideally a diagnostic would be generated when a composable function is placed more than
        once but such analysis requires the equivalent of Rust's borrow checker.

#### Alternate (B) considered: Ignore all but the first or last placement

With this option either the first or last placement would take precedence over all other
placements. If this option is chosen a justification of which to take would need to be
communicated to the developer.

*   `+` Simple to implement with similar complexity to throwing on second placement.
*   `-` Content might disappear unexpectedly when a content is placed more than once.
*   `-` Movable content behave differently than normal composable lambda in only one call will
        have an effect.
*   `-` Our guidance tells developers to not assume any execution order for composables, as a
        result "first placement" is strongly undefined if multiple placements are performed in a
        single Composition


#### Alternate (C) considered: Shared state

With this option all the state of each invocation of the movable content is shared between all
invocations. This, effectively, indirectly hoists the state objects in the movable conent for
use in multiple places in the composition.

The nodes themselves can be shared as the applier would be notified when it is requested to add a
node that appears elsewhere in the tree so that it can decide if the nodes should be cloned or
used directly. It would return whether it cloned the nodes to indicate to the composer that, for
cloned nodes, all changes to the nodes need to be repeated for the clones.

Part of this option would be the requirement that movable content could not be parameterized as
the node trees and states couldn't be reused if there was a possibility that the parameters could
be different at the call sites.

*   `+` Meets developer expectations that the state is identical for each call allowing multiple
        calls of movable content to be used as transitionary images. For example, multiple calls
        are made in a crossfade it is expected that the state would move to the retained
        composition. However, with the chosen above, the state would reset as the faded out image
        would retain the state as both exist in the composition simultaneously and the original
        caller takes precedence over the new caller.
*   `-` Complex to implement as there is a great deal of additional tracking that needs to be
        performed if nodes don't support shared nodes directly (which LayoutNodes currently do
        not).
*   `-` It is unclear what `WithConstraints` or any similar layout influenced composition would
        mean in such a scenario as the state is not be able to be shared in this case as the state
        would need to diverge. It is also unclear how similar divergence needs could be detected.
*   `-` It is unclear what `Modifiers` that expect to be singletons, such as focus, would mean as
        the effects of the singleton would appear in two places simultaneously. In some cases, such
        as a reflection, this is desirable but, in after-images, the expectation is that the effect
        of the state  is not shared.
*   `-` Movable content, as discussed above, would not be able to take parameters such as
        contextual  `Modifiers`. This requires additional nodes to explicitly be created around
        the placement to correctly reflect layout parameters to composition which we try to avoid
        by passing `Modifiers` as parameters.


## Issue 3: Compound hashcode

The compound hashcode is used by saved instance state to uniquely identify application state that
can be restored across Android lifecycle boundaries. The compound hashcode is calculated by
combining the hashcode of all the parent groups and sibling groups up to the point of the call to
produce the hashcode.

There are two options for calculating a compound hashcode, either a) have a hashcode that is
independent where the content is placed, or b) use the compound hashcode at the location of the
placement of the conent.

#### Chosen solution: Use the hashcode independent of where the content is placed.

In this option the compound hashcode is fixed at the time the movable content lambda is created.

*   `+` The compound hashcode is fixed as the movable content moves.
*   `-` Multiple placement of content will receive identical compound hashcodes.
*   `-` Differs significantly from a normal composition function.


#### Alternative consider: Use the hashcode relative to the placement

In this option the compound hashcode is based on its location where the content is placed which
implies that as it moves, the compound hashcode could change.

*   `-` The compound hashcode can change as the value placement moves. This might cause unexpected
        changes to state and cause `remember` and `rememberSaveable` behavior to be
        significantly different.
*   `+` Multiple placements of the same movable content will receive different compound hash codes.
*   `+` Behaves identically to a composition lambda called at the same location as the placement.

