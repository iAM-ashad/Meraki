import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

layout.buildDirectory.set(File("/sessions/blissful-tender-newton/meraki-build/app"))

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

        // Groq API key — read from local.properties; falls back to empty string so the
        // build never fails when the key is absent (e.g. CI without secrets).
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"${localProperties.getProperty("GROQ_API_KEY", "")}\""
        )
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
    // Phase 3: Prevent AGP from compressing .tflite model files inside the APK.
    // TensorFlow Lite's MappedByteBuffer loader requires the file to be byte-aligned
    // (uncompressed) so it can be memory-mapped directly from the APK.
    androidResources {
        noCompress += "tflite"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Return default values (0/null/false) for Android stubs (e.g. Log.d)
            // instead of throwing RuntimeException in JVM unit tests.
            isReturnDefaultValues = true
            // Increase heap for Robolectric + Compose UI tests that load image resources.
            all {
                it.maxHeapSize = "1512m"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

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
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.messaging)
    // firebase-analytics-ktx and firebase-crashlytics-ktx were removed from Firebase BOM 34.x;
    // their Kotlin extensions are now built into the main artifacts.
    implementation(libs.firebase.crashlytics)
    // Exoplayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // LiveData
    implementation(libs.androidx.runtime.livedata)
    // App Check: validates requests come from legitimate app instances.
    implementation(libs.firebase.appcheck.playintegrity)
    // Debug App Check provider — only included in debug builds; zero impact on release.
    debugImplementation(libs.firebase.appcheck.debug)
    // Animated Navigation
    implementation(libs.androidx.navigation.compose)
    // Pagination removed in Phase 6: paging-runtime-ktx and paging-compose had zero usages.
    // Material 3
    implementation(libs.material3)
    // Accompanist removed in Phase 3: pager migrated to Foundation pager;
    // navigation already used native NavHost (no Accompanist nav was needed).
    // Phase 3: On-Device Emotion Intelligence — ML Kit + TensorFlow Lite.
    // ML Kit Language ID: detect language of user text before TFLite inference.
    implementation(libs.mlkit.language.id)
    // TFLite core interpreter.
    // NOTE: Do NOT exclude tensorflow-lite-api or add it as compileOnly.
    // InterpreterImpl lives inside tensorflow-lite-api; removing it from the
    // runtime classpath causes a NoClassDefFoundError on app launch.
    // The earlier "manifest merger conflict" concern was a misdiagnosis —
    // a plain implementation dependency works correctly.
    implementation(libs.tensorflow.lite)

    /*implementation(libs.tensorflow.lite.support) {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
    }*/
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
    // Phase 1 Testing Foundation: 2026 testing stack
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    // Phase 2: Robolectric for Room in-memory DB tests (Android runtime on JVM)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Compose UI on the JVM test classpath: needed because LocalDataSource.kt initialises
    // gradientMap (which uses androidx.compose.ui.graphics.Color) at class-load time,
    // and the @Immutable annotation classes are referenced by the model types.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.graphics)
    // Phase 4: Compose UI Testing under Robolectric (createComposeRule, semantics, etc.)
    // ui-test contains all assertion/action extensions; ui-test-junit4 adds createComposeRule().
    testImplementation(libs.androidx.ui.test)
    testImplementation(libs.androidx.ui.test.junit4)
    // Phase 4: Type-safe navigation testing (TestNavHostController)
    testImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}