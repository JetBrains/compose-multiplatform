package com.google.r4a.adapters

// This class is a small helper class for creating objects that properly deal with controlled
// inputs. The expectation is that you will implement this class and have the implementation also
// be a listener object for whatever "change" event you are wanting to listen to. This class assumes
// that there is a "value" attribute and corresponding "onChange" attribute that you are passing
// directly into this class. This class will call the view's setter only when necessary, but will
// also ensure that the consumer of the view is properly calling recompose() and setting the view after
// an event gets fired, or else it will correctly "un-change" the view to whatever it was last set to.
abstract class InputController<V, T>(protected val view: V) {
    @Suppress("LeakingThis")
    private var lastSetValue: T = getValue()

    protected abstract fun getValue(): T
    protected abstract fun setValue(value: T)

    fun setValueIfNeeded(value: T) {
        val current = getValue()
        lastSetValue = value
        if (current != value) {
            setValue(value)
        }
    }

    protected fun afterChangeEvent(nextValue: T) {
        if (lastSetValue != nextValue && lastSetValue != getValue()) {
            setValueIfNeeded(lastSetValue)
        }
    }
}