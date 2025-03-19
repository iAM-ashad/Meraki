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

# ✅ Keep Firestore model classes
-keep class com.iamashad.meraki.model.** { *; }

# ✅ Keep Firebase Firestore annotations
-keepattributes *Annotation*

# ✅ Prevent Firestore from stripping generics
-keepattributes Signature

# ✅ Prevent Retrofit from stripping method parameters
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepattributes Signature

# ✅ Keep Gson serialization
-keep class com.google.gson.** { *; }
-keep class com.iamashad.meraki.model.** { *; }

# ✅ Prevent Dagger-Hilt from obfuscating dependency injection
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }
-keep class com.iamashad.meraki.di.** { *; }

# ✅ Prevent ViewModel obfuscation
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# ✅ Keep Navigation Components
-keep class androidx.navigation.** { *; }

# ✅ Prevent Firestore field names from being removed
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# ✅ Keep Retrofit service interfaces
-keep interface com.iamashad.meraki.network.** { *; }

# ✅ Prevent ProGuard from removing logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

