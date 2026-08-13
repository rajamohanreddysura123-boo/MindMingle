# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- MindMingle app: Firestore/kotlinx.serialization models ---
# These are (de)serialized by field name via reflection-free kotlinx.serialization,
# but keep them defensively since they cross the Firestore wire format boundary.
-keep,includedescriptorclasses class com.rajamohan.mindmingle.data.remote.dto.**{ *; }
-keep,includedescriptorclasses class com.rajamohan.mindmingle.domain.model.**{ *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Koin: our modules wire dependencies via explicit lambdas, not classpath
# scanning, so no reflection-based keep rules are needed beyond Koin's own
# bundled consumer-proguard-rules.pro.