// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin.android removed: AGP 9.0+ has built-in Kotlin support — applying it is now a fatal error.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // Phase 4: KSP replaces legacy-kapt for Hilt and Room annotation processing.
    alias(libs.plugins.ksp) apply false
    // Phase 4: Hilt plugin bumped to 2.57.1 to match hiltAndroid/hiltCompiler in catalog.
    // Phase 4: bumped 2.57.1 → 2.59.2; Hilt 2.59+ is the first release with AGP 9.0 support.
    // 2.59.2 fixes a ComponentTreeDeps artifact bug present in 2.59.0.
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}