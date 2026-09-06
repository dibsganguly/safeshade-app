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

# ============================================================================
# kotlinx.serialization  (cloud/dto)
# ============================================================================
#
# The cloud row DTOs are @Serializable, and kotlinx.serialization resolves a
# class's serializer at runtime through a synthetic `Companion.serializer()`
# and a `$serializer` inner class. R8 sees neither referenced from Kotlin
# source and strips both, and the failure is a runtime
# SerializationException on the first sync of a release build - never in
# debug, because minification is off there.
#
# isMinifyEnabled is currently false for release, so none of this bites today.
# It is written now precisely because it will not be noticed later: the pass
# that turns minification on will be about APK size, not about sync, and this
# would fail somewhere else entirely.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated serializers themselves.
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the DTOs' field names. @SerialName carries the wire name, but the
# generated serializer still reflects over the class.
-keep,includedescriptorclasses class com.safeshade.cloud.dto.** { *; }

# ============================================================================
# Ktor + OkHttp  (supabase-kt's transport)
# ============================================================================
#
# Ktor selects its engine through a ServiceLoader entry, so the OkHttp engine
# has no compile-time reference anywhere and R8 removes it. The symptom is
# "Failed to find HTTP client engine implementation" at the first request.
-keep class io.ktor.client.engine.okhttp.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

# OkHttp's own well-known suppressions; these classes are compile-only.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**