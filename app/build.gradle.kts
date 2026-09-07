import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Reads a key out of `local.properties`, or falls back to an environment
 * variable, or to [default].
 *
 * Why this exists rather than hard-coding the Supabase URL and anon key: the
 * anon key is not a secret in the "must never leak" sense (it is shipped in
 * every client and is only useful alongside row-level security), but the URL
 * and key together identify one person's project. Checking them into the repo
 * would point every clone of SafeShade at the same database.
 *
 * A blank value is a first-class state, not an error: `CloudContainer` reads
 * `BuildConfig.SUPABASE_URL` and, when it is blank, builds a disabled fake
 * client so the whole app still compiles, runs and is testable on a machine
 * that has never seen a Supabase project. `local.properties` is git-ignored and
 * is NOT created by this build.
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun prop(name: String, default: String = ""): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: default).trim()

android {
    namespace = "com.safeshade"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.safeshade"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "2.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cloud configuration. Blank => cloud disabled, and the app runs
        // entirely on-device exactly as it did before Phase 1. See prop() above.
        buildConfigField("String", "SUPABASE_URL", "\"${prop("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${prop("SUPABASE_ANON_KEY")}\"")
        // Google sign-in. The WEB client id, not the Android one - see
        // docs/wizards/google-signin.md for why that is not a typo.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${prop("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Persistence
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)

    // Weather (Open-Meteo) — the only network dependency
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Location + geofencing
    implementation(libs.play.services.location)

    // QR emergency card
    implementation(libs.zxing.core)

    // The safe-zone map. OpenStreetMap tiles, no API key and no account, and a
    // disk tile cache - which is the part that matters, because the screen this
    // replaces fetched Leaflet from a CDN at runtime and silently lost its tap
    // handler whenever that fetch failed.
    implementation(libs.osmdroid.android)

    // Cloud (Phase 1). Every supabase-kt module is versionless and takes its
    // version from the BOM; see the note in libs.versions.toml about the
    // Kotlin metadata ceiling.
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)
    // supabase-kt ships no HTTP engine of its own; without one, every call
    // fails at runtime with "no engine found" and nothing fails at compile.
    implementation(libs.ktor.client.okhttp)
    // Pinned so Ktor's engine and Retrofit share one OkHttp.
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Phase 2. Google sign-in via Credential Manager: the phone asks Google
    // for an ID token whose audience is the Web client ID, and hands it to
    // Supabase; no browser round-trip. Play Billing backs the cloud tiers
    // and reports its real result, which without a Console listing is an
    // error, and that error is what the plan screen shows.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.billing.ktx)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
