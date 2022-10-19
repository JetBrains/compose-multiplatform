import Todo

func simpleChildStack<T : AnyObject>(_ child: T) -> Value<ChildStack<AnyObject, T>> {
    return valueOf(
        ChildStack(
            active: ChildCreated(
                configuration: "config" as AnyObject,
                instance: child
            ),
            backStack: []
        )
    )
}
