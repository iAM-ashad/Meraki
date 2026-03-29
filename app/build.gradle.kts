import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    // kotlin.android removed: AGP 9.0+ has built-in Kotlin support — applying it is now a fatal error.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Phase 4: KSP replaces legacy-kapt for all annotation processors.
    alias(libs.plugins.ksp)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}

// Phase 6: geminiApiKey removed — firebase-ai (Firebase AI Logic) authenticates via Firebase, not API key.
val webClientId: String? = localProperties.getProperty("WEB_CLIENT_ID")

android {
    namespace = "com.iamashad.meraki"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iamashad.meraki"
        minSdk = 31
        targetSdk = 36
        versionCode = 6
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Phase 6: GEMINI_API_KEY buildConfigField removed — firebase-ai handles auth via Firebase.
        buildConfigField("String", "WEB_CLIENT_ID", "\"${webClientId}\"")
    }

    buildTypes {
        getByName("release") {
            ndk {
                debugSymbolLevel = "FULL"
            }
            isMinifyEnabled = false
            isShrinkResources = false
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
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// kotlin-stdlib-jdk7 and kotlin-stdlib-jdk8 were merged into kotlin-stdlib in Kotlin 1.8.0.
// Some transitive dependencies (e.g. Android test platform-runtime) still inject a stale
// 1.8.0 version constraint for these artifacts, which corrupts Gradle's binary resolution
// store. Force all three stdlib variants to the project's Kotlin version to align them.
val kotlinVersion = libs.versions.kotlin.get()
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    }
}

dependencies {

    //Glide
    implementation(libs.compose)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)       // Phase 4: KAPT → KSP
    implementation(libs.androidx.hilt.navigation.compose)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Json Parsing
    implementation(libs.gson)
    implementation(libs.json)
    // OkHTTP Logging
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    // Mock WebServer
    testImplementation(libs.mockwebserver)
    // Kotlin Serialization (required for type-safe Navigation routes — @Serializable)
    implementation(libs.kotlinx.serialization.json)
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // Lifecycle Scopes
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Phase 2: collectAsStateWithLifecycle() for UDF StateFlow collection in Composables
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)  // Phase 4: KAPT → KSP
    implementation(libs.androidx.room.ktx)
    // Datastore
    implementation(libs.androidx.datastore.preferences)
    // Lottie
    implementation(libs.lottie.compose)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation (libs.firebase.storage.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    // Exoplayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // LiveData
    implementation(libs.androidx.runtime.livedata)
    // Phase 5: Firebase AI Logic SDK — replaces deprecated com.google.ai.client.generativeai.
    // BOM-managed; no version pin needed. Supports googleAI() and vertexAI() backends.
    implementation(libs.firebase.ai)
    // Animated Navigation
    implementation(libs.androidx.navigation.compose)
    // Pagination removed in Phase 6: paging-runtime-ktx and paging-compose had zero usages.
    // Material 3
    implementation(libs.material3)
    // Accompanist removed in Phase 3: pager migrated to Foundation pager;
    // navigation already used native NavHost (no Accompanist nav was needed).
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // WindowSizeClass API
    implementation(libs.androidx.material3.window.size)
    // Adaptive Layout
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Material Icons: explicitly required since Material3 1.4.0 dropped the transitive dependency.
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}