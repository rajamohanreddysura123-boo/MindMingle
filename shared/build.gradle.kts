import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        // Suppress Beta warning for expect/actual classes (AppContext etc.)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    android {
       namespace = "com.rajamohan.mindmingle.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_17
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.play.services.auth)
            // Google Play In-App Updates (InAppUpdateManager.android.kt). Android-only artifact —
            // iOS has no equivalent (the App Store has no in-app update API) and desktop is
            // distributed outside any store, so both those targets have no update gate at all.
            implementation(libs.play.app.update)
            implementation(libs.firebase.messaging)
            // Google Mobile Ads (AdMob). Android-only artifact — iOS goes through
            // IosAdHost/AdMobBridge.swift, desktop has no SDK at all.
            implementation(libs.play.services.ads)
            // ML Kit Face Detection — gates profile photo uploads to images with a detectable
            // face (FaceDetector.android.kt). Android-only artifact; no ML Kit SDK exists for
            // iOS/desktop Kotlin targets, so those actuals skip the check (see FaceDetector.*.kt).
            implementation(libs.mlkit.face.detection)
            // Razorpay Checkout (MindMingle+ upgrade). Android-only artifact — iOS goes through
            // IosPaymentHost/RazorpayBridge.swift, desktop has no SDK at all.
            api(libs.razorpay.checkout)
            api(libs.razorpay.standardCore)
            // Coil's network layer, one HTTP engine per target — ktor picks whichever is on the
            // classpath, so no engine is ever named in common code.
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.functions)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.napier)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    // The Firebase BOM provides version numbers for native Firebase Android libs
    // that dev.gitlive:firebase-* pulls in as unversioned transitive dependencies.
    add("androidMainImplementation", platform(libs.firebase.bom))
}