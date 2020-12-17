import Todo

func simpleRouterState<T : AnyObject>(_ child: T) -> Value<RouterState<AnyObject, T>> {
    return valueOf(
        RouterState(
            activeChild: RouterStateEntryCreated(
                configuration: "config" as AnyObject,
                component: child
            ),
            backStack: []
        )
    )
}
