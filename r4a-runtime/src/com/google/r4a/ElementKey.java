package com.google.r4a;

import java.util.Objects;

/**
 * Created by jsproch on 7/14/17.
 */

public final class ElementKey {
    Class<?> type;
    Object key;

    public ElementKey(Class<?> type, Object key) {
        this.type = type;
        this.key = key;
    }

    @Override
    public int hashCode() {
        return type.hashCode()+key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        ElementKey other = (ElementKey) obj;
        return Objects.equals(this.type, other.type) && Objects.equals(this.key, other.key);
    }
}
