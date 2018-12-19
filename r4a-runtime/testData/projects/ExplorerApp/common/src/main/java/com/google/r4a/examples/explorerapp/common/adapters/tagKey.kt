package com.google.r4a.examples.explorerapp.common.adapters

/**
 * This function will take in a string and pass back a valid resource identifier for View.setTag(...). We should eventually move this
 * to a resource id that's actually generated via AAPT but doing that in this project is proving to be complicated, so for now I'm
 * just doing this as a stop-gap.
 */
internal fun tagKey(key: String): Int {
    return (3 shl 24) or key.hashCode()
}