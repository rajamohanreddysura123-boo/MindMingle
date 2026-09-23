import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    // The FCM service has to live in this module — a manifest can only name a class from the
    // application it belongs to — so the app needs its own view of firebase-messaging. The BOM
    // comes with it because the artifact is declared without a version.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.rajamohan.mindmingle"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rajamohan.mindmingle"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.0.2"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // A universal APK was carrying all four ABIs' native libs (Firestore's gRPC core, Skia,
    // ML Kit) at once — x86/x86_64 alone were ~18MB dead weight on every real device, which only
    // ever runs one ABI. No real phone in this app's market ships x86; armeabi-v7a stays for
    // older 32-bit devices still inside minSdk 29. Play's App Bundle path already does this kind
    // of per-device delivery on its own — this is what makes a directly-installed/sideloaded APK
    // match that.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Native crashes (ML Kit, Firebase, Play Services ads all ship .so files) reach Play
            // as raw addresses without this — the bundle carries a symbol file so they symbolicate.
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}