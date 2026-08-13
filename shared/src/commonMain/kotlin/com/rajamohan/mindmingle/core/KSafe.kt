package com.rajamohan.mindmingle.core

/**
 * KSafe — suspend-based key-value store contract.
 * Injected via Koin into [LMPreferences].
 * Concrete implementations are provided per platform in the DI module.
 */
interface KSafe {

    suspend fun put(key: String, value: String)
    suspend fun put(key: String, value: Int)
    suspend fun put(key: String, value: Boolean)

    suspend fun get(key: String, defaultValue: String): String
    suspend fun get(key: String, defaultValue: Int): Int
    suspend fun get(key: String, defaultValue: Boolean): Boolean

    suspend fun delete(key: String)
    suspend fun clearAll()
    suspend fun close()
}
