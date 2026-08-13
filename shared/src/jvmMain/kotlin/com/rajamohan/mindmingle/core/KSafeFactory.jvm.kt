package com.rajamohan.mindmingle.core

import java.util.prefs.Preferences

actual fun createKSafe(): KSafe = JvmKSafe()

private class JvmKSafe : KSafe {

    private val preferences: Preferences = Preferences.userRoot().node("com/rajamohan/mindmingle/ksafe")

    override suspend fun put(key: String, value: String) {
        preferences.put(key, value)
        preferences.flush()
    }

    override suspend fun put(key: String, value: Int) {
        preferences.putInt(key, value)
        preferences.flush()
    }

    override suspend fun put(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
        preferences.flush()
    }

    override suspend fun get(key: String, defaultValue: String): String =
        preferences.get(key, defaultValue)

    override suspend fun get(key: String, defaultValue: Int): Int =
        preferences.getInt(key, defaultValue)

    override suspend fun get(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override suspend fun delete(key: String) {
        preferences.remove(key)
        preferences.flush()
    }

    override suspend fun clearAll() {
        preferences.clear()
        preferences.flush()
    }

    override suspend fun close() {
        preferences.flush()
    }
}
