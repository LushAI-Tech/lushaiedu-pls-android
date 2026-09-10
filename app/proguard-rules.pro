# LushAIEdu — release shrinking rules
# Keeps Retrofit/kotlinx-serialization DTOs and API interfaces working after R8.

# Better crash reports from release builds
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlin.**
-dontwarn kotlinx.**

# BuildConfig (API_BASE_URL, GOOGLE_WEB_CLIENT_ID)
-keep class com.lushaiedupls.BuildConfig { *; }

# --- Retrofit + OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-keep,allowobfuscation,allowshrinking interface com.lushaiedupls.data.remote.api.** { *; }

-keepclasseswithmembers,allowobfuscation,allowshrinking class * {
    @retrofit2.http.* <methods>;
}

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- kotlinx.serialization (API request/response bodies) ---
-keepattributes *Annotation*, InnerClasses

-dontnote kotlinx.serialization.**

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if class **$Companion {
    static **$Companion Companion;
}
-keepclassmembers class <2> {
    public kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
    public static ** serializer(...);
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    public static ** serializer(...);
}

-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class com.lushaiedupls.data.remote.dto.** {
    <fields>;
    <init>(...);
}

-keep,allowobfuscation,allowshrinking class com.lushaiedupls.data.remote.dto.**$$serializer {
    <init>(...);
    public kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers enum com.lushaiedupls.data.remote.dto.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Network helpers that parse error JSON at runtime
-keep class com.lushaiedupls.data.remote.NetworkResult { *; }
-keep class com.lushaiedupls.data.remote.NetworkResult* { *; }

# --- Firebase / FCM ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Google Sign-In / Identity
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn com.google.android.libraries.identity.**

# Coil (avatars)
-dontwarn coil.**

# KaTeX WebView bridge (AI chat / quiz formulas)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ZXing (QR)
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
