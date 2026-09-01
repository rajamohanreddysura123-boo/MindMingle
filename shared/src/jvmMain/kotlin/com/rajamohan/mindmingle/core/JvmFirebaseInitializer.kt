package com.rajamohan.mindmingle.core

import android.app.Application
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import java.io.File
import java.util.Properties

/**
 * The JVM/Desktop Firebase target (dev.gitlive:firebase-*-jvm, backed by firebase-java-sdk)
 * is a client-style REST reimplementation, not the Admin SDK — it is bound by Firestore
 * Security Rules exactly like Android, and needs an explicit [Firebase.initialize] call
 * (there is no google-services.json auto-init on JVM). Must run before any Firestore/Auth call.
 */
object JvmFirebaseInitializer {

    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // firebase-java-sdk's Android-shim code (Log, SharedPreferences-equivalent) needs a
            // storage/log backend registered before Firebase.initialize, or it NPEs on first use.
            // File-backed (not in-memory) so a signed-in Auth session survives a desktop app restart,
            // same as the persisted session on Android/iOS.
            FirebasePlatform.initializeFirebasePlatform(
                FileBackedFirebasePlatform(
                    File(System.getProperty("user.home"), ".mindmingle/firebase-jvm-storage.properties")
                )
            )

            Firebase.initialize(
                // Must be Application, not a bare Context: Firestore's AndroidConnectivityMonitor
                // casts context.applicationContext to Application, and the stub Context returns
                // itself from applicationContext, which would throw ClassCastException.
                context = Application(),
                options = FirebaseOptions(
                    applicationId = "1:532395068238:android:d2f8ca7dc7c361f2f46c1f",
                    apiKey = "AIzaSyAWyx_YOnFTCCNlFkWMlrOjtrc9cF8VgLc",
                    projectId = "tech-connect-44987",
                    storageBucket = "tech-connect-44987.firebasestorage.app",
                    gcmSenderId = "532395068238",
                    authDomain = "tech-connect-44987.firebaseapp.com"
                )
            )
            initialized = true
        }
    }
}

private class FileBackedFirebasePlatform(private val file: File) : FirebasePlatform() {
    private val props = Properties().apply {
        if (file.exists()) file.inputStream().use { load(it) }
    }

    override fun store(key: String, value: String) {
        synchronized(props) {
            props.setProperty(key, value)
            persist()
        }
    }

    override fun retrieve(key: String): String? = synchronized(props) { props.getProperty(key) }

    override fun clear(key: String) {
        synchronized(props) {
            props.remove(key)
            persist()
        }
    }

    override fun log(msg: String) = println(msg)

    private fun persist() {
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, null) }
    }
}
