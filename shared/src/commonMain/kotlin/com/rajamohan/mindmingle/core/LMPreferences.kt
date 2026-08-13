package com.rajamohan.mindmingle.core

import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class LMPreferences : KoinComponent {
    private val kSafe : KSafe by inject()

    internal suspend fun <T : Any> update(
        key: String,
        value: T,
        type: KClass<T>
    ) {
        when (type) {
            String::class -> kSafe.put(key, value as String)
            Int::class -> kSafe.put(key, value as Int)
            Boolean::class -> kSafe.put(key, value as Boolean)
            else -> throw Exception("Unknow class please record the entry.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun <T : Any> get(
        key: String,
        defaultValue: T,
        value: KClass<T>
    ): T {
        return when (value) {
            String::class -> kSafe.get(key, defaultValue as String) as T
            Int::class -> kSafe.get(key, defaultValue as Int) as T
            Boolean::class -> kSafe.get(key, defaultValue as Boolean) as T
            else -> throw Exception("Unknow class please record the entry.")
        }
    }

    internal suspend fun delete(
        key: String
    ) {
        kSafe.delete(
            key = key
        )
    }

    internal suspend fun clear() {
        kSafe.clearAll()
        kSafe.close()
    }
}