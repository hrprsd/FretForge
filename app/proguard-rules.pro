# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Haraprasad\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom rules here.

# Keep data models intact for GSON serialization/deserialization and Room
-keep class com.fretforge.data.** { *; }
-keepclassmembers class com.fretforge.data.** { *; }

# Keep GSON library classes
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
