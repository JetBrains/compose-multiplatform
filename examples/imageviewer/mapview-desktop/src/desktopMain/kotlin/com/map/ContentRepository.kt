package com.map

interface ContentRepository<K, T> {
    suspend fun loadContent(key: K): T
}
