package com.rajamohan.mindmingle.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STORE_NAME = "oo_ksafe"

actual fun createKSafe(): KSafe = AndroidKSafe()

private class AndroidKSafe : KSafe {

    private val preferences: SharedPreferences?
        get() = (AppContext.get() as? Context)
            ?.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    private val fallback = mutableMapOf<String, Any>()

    override suspend fun put(key: String, value: String) = write(key, value) { it.putString(key, value) }

    override suspend fun put(key: String, value: Int) = write(key, value) { it.putInt(key, value) }

    override suspend fun put(key: String, value: Boolean) = write(key, value) { it.putBoolean(key, value) }

    override suspend fun get(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        preferences?.getString(key, defaultValue) ?: fallback[key] as? String ?: defaultValue
    }

    override suspend fun get(key: String, defaultValue: Int): Int = withContext(Dispatchers.IO) {
        preferences?.getInt(key, defaultValue) ?: fallback[key] as? Int ?: defaultValue
    }

    override suspend fun get(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.IO) {
        preferences?.getBoolean(key, defaultValue) ?: fallback[key] as? Boolean ?: defaultValue
    }

    override suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            fallback.remove(key)
            preferences?.edit()?.remove(key)?.apply()
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            fallback.clear()
            preferences?.edit()?.clear()?.apply()
        }
    }

    override suspend fun close() = Unit

    private suspend fun write(key: String, value: Any, block: (SharedPreferences.Editor) -> Unit) {
        withContext(Dispatchers.IO) {
            val editor = preferences?.edit()
            if (editor == null) {
                fallback[key] = value
            } else {
                block(editor)
                editor.apply()
            }
        }
    }
}
