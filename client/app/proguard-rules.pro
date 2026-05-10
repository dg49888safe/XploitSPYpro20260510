# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/king/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========== Socket.IO 客户端 ==========
-keep class io.socket.** { *; }
-keep interface io.socket.** { *; }
-keepclassmembers class io.socket.** { *; }
-dontwarn io.socket.**

# ========== OkHttp ==========
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-dontwarn okhttp3.**

# ========== Okio ==========
-keep class okio.** { *; }
-keep interface okio.** { *; }
-keepclassmembers class okio.** { *; }
-dontwarn okio.**

# ========== 应用相关 ==========
-keep class com.remote.app.** { *; }
-keep interface com.remote.app.** { *; }
-keepclassmembers class com.remote.app.** { *; }

# ========== AndroidX ==========
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# ========== JSON 处理 ==========
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# ========== 其他 ==========
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# 保留行号用于调试
-renamesourcefileattribute SourceFile

# 最小化冗余
-repackageclasses
-allowaccessmodification

# ========== Native 方法 ==========
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== Enum ==========
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== View 构造函数 ==========
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
