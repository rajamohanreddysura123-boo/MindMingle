package com.rajamohan.mindmingle.core

import platform.Foundation.NSUserDefaults

actual fun createKSafe(): KSafe = IosKSafe()

private class IosKSafe : KSafe {

    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun put(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override suspend fun put(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
    }

    override suspend fun put(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override suspend fun get(key: String, defaultValue: String): String =
        defaults.stringForKey(key) ?: defaultValue

    override suspend fun get(key: String, defaultValue: Int): Int =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key).toInt()

    override suspend fun get(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override suspend fun delete(key: String) {
        defaults.removeObjectForKey(key)
    }

    override suspend fun clearAll() {
        val domain = defaults.dictionaryRepresentation()
        for (key in domain.keys) {
            defaults.removeObjectForKey(key as String)
        }
    }

    override suspend fun close() {
        defaults.synchronize()
    }
}
